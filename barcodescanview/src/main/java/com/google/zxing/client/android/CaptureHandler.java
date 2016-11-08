package com.google.zxing.client.android;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraManager;

/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class CaptureHandler extends Handler {
  private final DecodeThread decodeThread;
  private final CameraManager cameraManager;
  private final OnCaptureListener onCaptureListener;

  public CaptureHandler(ViewfinderView viewfinderView, CameraManager cameraManager,
      OnCaptureListener l) {
    this.decodeThread = new DecodeThread(viewfinderView, cameraManager, this);
    decodeThread.start();
    this.cameraManager = cameraManager;
    this.onCaptureListener = l;
  }

  @Override public void handleMessage(Message msg) {
    switch (msg.what) {
      case Constants.MESSAGE_SUCCEEDED:
        Result result = (Result) msg.obj;
        if (onCaptureListener != null) {
          onCaptureListener.onCapture(result);
        }
        break;

      case Constants.MESSAGE_FAILED:
        restartPreviewAndDecode();
        break;
    }
  }

  /**
   * Interface defination for a callback to be invoked when bar code result was captured.
   */
  public interface OnCaptureListener {
    /**
     * Invoked when bar code result was captured.
     *
     * @param result {@link Result}
     */
    void onCapture(Result result);
  }

  /**
   * Restart preview.
   */
  public void restartPreviewAndDecode() {
    cameraManager.requestPreviewFrame(decodeThread.getHandler(), Constants.MESSAGE_DECODE);
  }

  /**
   * Quit Synchronously.
   */
  public void quitSynchronously() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      decodeThread.quitSafely();
    } else {
      decodeThread.quit();
    }

    try {
      decodeThread.join(500L);
    } catch (InterruptedException ignore) {
    }

    removeMessages(Constants.MESSAGE_SUCCEEDED);
    removeMessages(Constants.MESSAGE_FAILED);
  }
}
