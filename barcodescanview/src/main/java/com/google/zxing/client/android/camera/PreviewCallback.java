package com.google.zxing.client.android.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;

/**
 * This class represents a {@link Camera.PreviewCallback}.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@SuppressWarnings("deprecation") final class PreviewCallback implements Camera.PreviewCallback {
  private Handler handler;
  private int previewMessage;

  @Override public void onPreviewFrame(byte[] data, Camera camera) {
    Message message = Message.obtain(handler, previewMessage, data);
    message.sendToTarget();
  }

  public void setHandler(Handler handler, int message) {
    this.handler = handler;
    this.previewMessage = message;
  }
}
