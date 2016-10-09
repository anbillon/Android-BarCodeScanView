package com.tourbillon.barcodescanview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.google.zxing.Result;
import com.google.zxing.client.android.AmbientLightManager;
import com.google.zxing.client.android.BeepManager;
import com.google.zxing.client.android.CaptureHandler;
import com.google.zxing.client.android.ViewfinderView;
import com.google.zxing.client.android.camera.CameraManager;
import java.io.IOException;

/**
 * BarCodeScanView Class which uses ZXING lib and let you easily integrate a QR decoder view.
 * Take some classes and made some modifications in the original ZXING - Barcode Scanner project.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public class BarCodeScanView extends FrameLayout
    implements SurfaceHolder.Callback, CaptureHandler.OnCaptureListener {
  private OnCameraErrorListener onCameraErrorListener;
  private OnBarCodeReadListener onBarCodeReadListener;
  private final CameraManager cameraManager;
  private final BeepManager beepManager;
  private final AmbientLightManager ambientLightManager;
  private CaptureHandler captureHandler;

  public BarCodeScanView(Context context) {
    this(context, null);
  }

  public BarCodeScanView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public BarCodeScanView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    setKeepScreenOn(true);

    SurfaceView surfaceView = new SurfaceView(context, attrs, defStyleAttr);
    ViewfinderView viewfinderView = new ViewfinderView(context, attrs);
    LayoutParams params =
        new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    surfaceView.setLayoutParams(params);
    viewfinderView.setLayoutParams(params);

    addView(surfaceView);
    addView(viewfinderView);

    beepManager = new BeepManager(context);
    ambientLightManager = new AmbientLightManager(context);
    cameraManager = new CameraManager(getContext());
    viewfinderView.setCameraManager(cameraManager);
    //ambientLightManager.start(cameraManager);
    surfaceView.getHolder().addCallback(this);
    captureHandler = new CaptureHandler(viewfinderView, cameraManager, this);
  }

  @Override public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    setKeepScreenOn(false);
    beepManager.close();
    //ambientLightManager.stop();
    captureHandler.quitSynchronously();
    captureHandler = null;
  }

  @Override public void surfaceCreated(SurfaceHolder holder) {
    try {
      cameraManager.openDriver(holder, this.getWidth(), this.getHeight());
    } catch (IOException e) {
      if (onCameraErrorListener != null) {
        onCameraErrorListener.onCameraError("Can not openDriver: " + e.getMessage());
      }
      return;
    }

    cameraManager.startPreview();
    captureHandler.restartPreviewAndDecode();
  }

  @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    if (holder.getSurface() == null) {
      if (onCameraErrorListener != null) {
        onCameraErrorListener.onCameraError("Error: preview surface does not exist");
      }
      return;
    }

    if (cameraManager.getPreviewSize() == null) {
      if (onCameraErrorListener != null) {
        onCameraErrorListener.onCameraError("Error: preview size does not exist");
      }
      return;
    }

    cameraManager.stopPreview();
    cameraManager.startPreview();
    captureHandler.restartPreviewAndDecode();
  }

  @Override public void surfaceDestroyed(SurfaceHolder holder) {
    cameraManager.stopPreview();
    cameraManager.closeDriver();
  }

  @Override public void onCapture(Result result) {
    beepManager.playBeepSoundAndVibrate();
    if (onBarCodeReadListener != null) {
      onBarCodeReadListener.onBarCodeRead(result);
    }
  }

  /**
   * Interface defination for a callback to be invoked when opening camera error.
   */
  public interface OnCameraErrorListener {
    /**
     * Invoked when opening camera error.
     */
    void onCameraError(String errorMsg);
  }

  /**
   * Interface defination for a callback to be invoked when bar code decoded.
   */
  public interface OnBarCodeReadListener {
    /**
     * Invoked when bar code devoded.
     *
     * @param result {@link Result}
     */
    void onBarCodeRead(Result result);
  }

  /**
   * Set on camera error listener.
   *
   * @param l {@link OnCameraErrorListener}
   */
  public void setOnCameraErrorListener(OnCameraErrorListener l) {
    this.onCameraErrorListener = l;
  }

  /**
   * Set the callback to return decoding result
   *
   * @param onBarCodeReadListener the listener
   */
  public void setOnBarCodeReadListener(OnBarCodeReadListener onBarCodeReadListener) {
    this.onBarCodeReadListener = onBarCodeReadListener;
  }

  /**
   * Should play beep and vibrate or not. Default is true.
   *
   * @param flag flag
   */
  public void setShouldPlayBeepAndVibrate(boolean flag) {
    beepManager.shouldPlayBeepAndVbirate(flag);
  }

  /**
   * Set Camera autofocus interval value default value is 5000 ms.
   *
   * @param autofocusIntervalInMs autofocus interval value
   */
  public void setAutofocusInterval(long autofocusIntervalInMs) {
    if (cameraManager != null) {
      cameraManager.setAutoFocusInterval(autofocusIntervalInMs);
    }
  }

  /**
   * Set Torch enabled/disabled.
   * default value is false
   *
   * @param enabled torch enabled/disabled.
   */
  public void setTorchEnabled(boolean enabled) {
    if (cameraManager != null) {
      cameraManager.setTorch(enabled);
    }
  }

  /**
   * Restart preview and decode.
   */
  public void restart() {
    captureHandler.restartPreviewAndDecode();
  }
}
