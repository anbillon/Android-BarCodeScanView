/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.client.android.camera.open.OpenCamera;
import com.google.zxing.client.android.camera.open.OpenCameraInterface;
import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
@SuppressWarnings("deprecation") public final class CameraManager {
  private static final String TAG = CameraManager.class.getSimpleName();
  private static final int MIN_FRAME_WIDTH = 240;
  private static final int MIN_FRAME_HEIGHT = 240;
  private static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
  private static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080

  private final CameraConfigurationManager configManager;
  private OpenCamera openCamera;
  private AutoFocusManager autoFocusManager;
  private boolean initialized;
  private boolean previewing;
  private Rect framingRect;
  private Rect framingRectInPreview;
  private PreviewCallback previewCallback;

  private int requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA;
  private long autoFocusIntervalInMs = AutoFocusManager.DEFAULT_AUTO_FOCUS_INTERVAL_MS;

  public CameraManager(Context context) {
    this.configManager = new CameraConfigurationManager(context);
    previewCallback = new PreviewCallback();
  }

  /**
   * Set auto focus interval.
   *
   * @param autofocusIntervalInMs auto focus interval in millisecond
   */
  public void setAutoFocusInterval(long autofocusIntervalInMs) {
    this.autoFocusIntervalInMs = autofocusIntervalInMs;
    if (autoFocusManager != null) {
      autoFocusManager.setAutofocusInterval(autofocusIntervalInMs);
    }
  }

  /**
   * Get camera preview size.
   *
   * @return {@link Point}
   */
  public Point getPreviewSize() {
    return configManager.getCameraResolution();
  }

  /**
   * Opens the camera driver and initializes the hardware parameters.
   *
   * @param holder The surface object which the camera will draw preview frames into.
   * @param height @throws IOException Indicates the camera driver failed to open.
   */
  public synchronized void openDriver(SurfaceHolder holder, int width, int height)
      throws IOException {
    OpenCamera theCamera = openCamera;
    if (!isOpen()) {
      theCamera = OpenCameraInterface.open(requestedCameraId);
      if (theCamera == null || theCamera.getCamera() == null) {
        throw new IOException("Camera.open() failed to return object from driver");
      }
      openCamera = theCamera;
    }

    if (!initialized) {
      initialized = true;
      configManager.initFromCameraParameters(theCamera, width, height);
    }

    Camera cameraObject = theCamera.getCamera();
    Camera.Parameters parameters = cameraObject.getParameters();
    /* save these, temporarily */
    String parametersFlattened = parameters == null ? null : parameters.flatten();
    try {
      configManager.setDesiredCameraParameters(theCamera, false);
    } catch (RuntimeException re) {
      // Driver failed
      Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
      Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
      // Reset:
      if (parametersFlattened != null) {
        parameters = cameraObject.getParameters();
        parameters.unflatten(parametersFlattened);
        try {
          cameraObject.setParameters(parameters);
          configManager.setDesiredCameraParameters(theCamera, true);
        } catch (RuntimeException exp) {
          // Well, darn. Give up
          Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
        }
      }
    }

    cameraObject.setPreviewDisplay(holder);
  }

  /**
   * Allows third party apps to specify the camera ID, rather than determine
   * it automatically based on available cameras and their orientation.
   *
   * @param cameraId camera ID of the camera to use. A negative value means "no preference".
   */
  public synchronized void setPreviewCameraId(int cameraId) {
    requestedCameraId = cameraId;
  }

  public int getPreviewCameraId() {
    return requestedCameraId;
  }

  /**
   * Set torch light on or off.
   *
   * @param enabled if {@code true}, light should be turned on if currently off. And vice versa.
   */
  public synchronized void setTorch(boolean enabled) {
    OpenCamera theCamera = openCamera;
    if (theCamera != null) {
      if (enabled != configManager.getTorchState(theCamera.getCamera())) {
        boolean wasAutoFocusManager = autoFocusManager != null;
        if (wasAutoFocusManager) {
          autoFocusManager.stop();
          autoFocusManager = null;
        }
        configManager.setTorchEnabled(theCamera.getCamera(), enabled);
        if (wasAutoFocusManager) {
          autoFocusManager = new AutoFocusManager(theCamera.getCamera());
          autoFocusManager.start();
        }
      }
    }
  }

  /**
   * To check if current camera is opened.
   *
   * @return true if opened, otherwise return false
   */
  public synchronized boolean isOpen() {
    return openCamera != null && openCamera.getCamera() != null;
  }

  /**
   * Closes the camera driver if still in use.
   */
  public synchronized void closeDriver() {
    if (isOpen()) {
      openCamera.getCamera().release();
      openCamera = null;
      /*
       * make sure to clear these each time we close the camera, so that any scanning rect
       * requested by intent is forgotten
       */
      framingRect = null;
      framingRectInPreview = null;
    }
  }

  /**
   * Asks the camera hardware to begin drawing preview frames to the screen.
   */
  public synchronized void startPreview() {
    OpenCamera theCamera = openCamera;
    if (theCamera != null && !previewing) {
      theCamera.getCamera().startPreview();
      previewing = true;
      autoFocusManager = new AutoFocusManager(theCamera.getCamera());
      autoFocusManager.setAutofocusInterval(autoFocusIntervalInMs);
    }
  }

  /**
   * Tells the camera to stop drawing preview frames.
   */
  public synchronized void stopPreview() {
    if (autoFocusManager != null) {
      autoFocusManager.stop();
      autoFocusManager = null;
    }
    if (openCamera != null && previewing) {
      openCamera.getCamera().stopPreview();
      previewing = false;
    }
  }

  /**
   * A single preview frame will be returned to the handler supplied. The data will arrive as
   * byte[] in the message.obj field, respectively.
   *
   * @param handler The handler to send the message to.
   * @param message The what field of the message to be sent.
   */
  public synchronized void requestPreviewFrame(Handler handler, int message) {
    OpenCamera theCamera = openCamera;
    if (theCamera != null && previewing) {
      previewCallback.setHandler(handler, message);
      theCamera.getCamera().setOneShotPreviewCallback(previewCallback);
    }
  }

  /**
   * Calculates the framing rect which the UI should draw to show the user where to place the
   * barcode. This target helps with alignment as well as forces the user to hold the device
   * far enough away to ensure the image will be in focus.
   *
   * @return The rectangle to draw on screen in window coordinates.
   */
  public synchronized Rect getFramingRect() {
    if (framingRect == null) {
      if (openCamera == null) {
        return null;
      }
      Point screenResolution = configManager.getScreenResolution();
      if (screenResolution == null) {
        /* called early, before init even finished */
        return null;
      }

      int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
      int height =
          findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

      int leftOffset = (screenResolution.x - width) / 2;
      int topOffset = (screenResolution.y - height) / 2;
      framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
      Log.d(TAG, "Calculated framing rect: " + framingRect);
    }

    return framingRect;
  }

  private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
    int dim = 5 * resolution / 8; // Target 5/8 of each dimension
    if (dim < hardMin) {
      return hardMin;
    }
    if (dim > hardMax) {
      return hardMax;
    }
    return dim;
  }

  /**
   * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
   * not UI / screen.
   *
   * @return {@link Rect} expressing barcode scan area in terms of the preview size
   */
  public synchronized Rect getFramingRectInPreview() {
    if (framingRectInPreview == null) {
      Rect framingRect = getFramingRect();
      if (framingRect == null) {
        return null;
      }
      Rect rect = new Rect(framingRect);
      Point cameraResolution = configManager.getCameraResolution();
      Point screenResolution = configManager.getScreenResolution();
      if (cameraResolution == null || screenResolution == null) {
        /* called early, before init even finished */
        return null;
      }

      /* rotate the frame rect in preview */
      float scaleX = cameraResolution.x * 1f / screenResolution.x;
      float scaleY = cameraResolution.y * 1f / screenResolution.y;
      rect.left = (int) (rect.left * scaleY);
      rect.right = (int) (rect.right * scaleY);
      rect.top = (int) (rect.top * scaleX);
      rect.bottom = (int) (rect.bottom * scaleX);
      framingRectInPreview = rect;
    }

    return framingRectInPreview;
  }

  /**
   * Check if this device has a camera.
   *
   * @param context context to use
   * @return true if camera available, otherwise return false
   */
  public static boolean checkCameraHardware(Context context) {
    return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)
        || context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        || Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN && context.getPackageManager()
        .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
  }

  /**
   * A factory method to build the appropriate LuminanceSource object based on the format
   * of the preview buffers, as described by Camera.Parameters.
   *
   * @param data A preview frame.
   * @param width The width of the image.
   * @param height The height of the image.
   * @return A PlanarYUVLuminanceSource instance.
   */
  public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
    Rect rect = getFramingRectInPreview();
    if (rect == null) {
      return null;
    }

    return new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
  }
}
