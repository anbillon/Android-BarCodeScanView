package com.google.zxing.client.android;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.android.camera.CameraManager;

/**
 * This class represents a {@link Handler} used in {@link DecodeThread}.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
final class DecodeHandler extends Handler {
  private final MultiFormatReader multiFormatReader;
  private final CameraManager cameraManager;
  private final Handler handler;

  DecodeHandler(CameraManager cameraManager, MultiFormatReader multiFormatReader,
      CaptureHandler captureHandler, Looper looper) {
    super(looper);
    this.cameraManager = cameraManager;
    this.multiFormatReader = multiFormatReader;
    handler = captureHandler;
  }

  @Override public void handleMessage(Message msg) {
    switch (msg.what) {
      case Constants.MESSAGE_DECODE:
        decode((byte[]) msg.obj);
        break;
    }
  }

  @SuppressWarnings("SuspiciousNameCombination") private void decode(byte[] data) {
    if (cameraManager.getPreviewSize() == null) {
      Message message = Message.obtain(handler, Constants.MESSAGE_FAILED);
      message.sendToTarget();
      return;
    }

    int width = cameraManager.getPreviewSize().x;
    int height = cameraManager.getPreviewSize().y;

      /* rotate the data cause width and height is opposite  */
    byte[] rotatedData = new byte[data.length];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++)
        rotatedData[x * height + height - y - 1] = data[x + y * width];
    }

    final PlanarYUVLuminanceSource source =
        cameraManager.buildLuminanceSource(rotatedData, height, width);
    Result rawResult = null;

    if (source != null) {
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      try {
        rawResult = multiFormatReader.decodeWithState(bitmap);
      } catch (ReaderException ignore) {
        /* continue */
      } finally {
        multiFormatReader.reset();
      }
    }

    if (rawResult != null) {
      Message message = Message.obtain(handler, Constants.MESSAGE_SUCCEEDED, rawResult);
      message.sendToTarget();
    } else {
      Message message = Message.obtain(handler, Constants.MESSAGE_FAILED);
      message.sendToTarget();
    }
  }
}
