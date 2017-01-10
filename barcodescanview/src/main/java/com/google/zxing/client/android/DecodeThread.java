package com.google.zxing.client.android;

import android.os.Handler;
import android.os.HandlerThread;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.android.camera.CameraManager;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * This class represents a {@link Thread} for bar code decoding.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
final class DecodeThread extends HandlerThread {
  private final CameraManager cameraManager;
  private final MultiFormatReader multiFormatReader;
  private final CaptureHandler captureHandler;
  private DecodeHandler decodeHandler;

  private DecodeThread(String name, ViewfinderView viewfinderView, CameraManager cameraManager,
      CaptureHandler captureHandler) {
    super(name);

    multiFormatReader = new MultiFormatReader();
    Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
    Collection<BarcodeFormat> decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
    decodeFormats.addAll(DecodeFormatManager.ALL_FORMATS);
    hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
    hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK,
        new ViewfinderResultPointCallback(viewfinderView, cameraManager));
    multiFormatReader.setHints(hints);
    this.cameraManager = cameraManager;
    this.captureHandler = captureHandler;
  }

  DecodeThread(ViewfinderView viewfinderView, CameraManager cameraManager,
      CaptureHandler captureHandler) {
    this("DecodeThread", viewfinderView, cameraManager, captureHandler);
  }

  @Override protected void onLooperPrepared() {
    super.onLooperPrepared();
    decodeHandler =
        new DecodeHandler(cameraManager, multiFormatReader, captureHandler, getLooper());
  }

  /**
   * Get {@link DecodeHandler} to use.
   *
   * @return {@link DecodeHandler}
   */
  Handler getHandler() {
    return decodeHandler;
  }
}
