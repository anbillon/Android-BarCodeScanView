package com.google.zxing.client.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import java.util.Hashtable;

/**
 * Encoder
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class Encoder {
  private static final Hashtable<EncodeHintType, String> HINTS = new Hashtable<>();

  static {
    HINTS.put(EncodeHintType.CHARACTER_SET, "utf-8");
  }

  /**
   * Generate bar code with text below.
   *
   * @param content context to encode
   * @param width width of bar code
   * @param height height of bar code
   * @param showText show text or not
   * @param context context to use
   * @return bar code bitmap
   */
  public static Bitmap generateBarCodeBitmap(String content, int width, int height,
      boolean showText, Context context) {
    Bitmap result = generateBarcodeBitmap(content, width, height);
    if (showText) {
      Bitmap text = getCodeBitmap(content, width + 40, height / 3, context);
      result = mergeBitmap(result, text, new PointF(0, height));
    }
    return result;
  }

  /**
   * Generate bar code.
   *
   * @param content content to encode
   * @param width width of bar code
   * @param height height of bar code
   * @return bar code bitmap
   */
  public static Bitmap generateBarcodeBitmap(String content, int width, int height) {
    MultiFormatWriter writer = new MultiFormatWriter();
    Bitmap bitmap = null;
    try {
      BitMatrix result = writer.encode(content, BarcodeFormat.CODE_128, width, height, HINTS);
      int[] pixels = new int[width * height];
      for (int y = 0; y < height; y++) {
        int offset = y * width;
        for (int x = 0; x < width; x++) {
          pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
        }
      }
      bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    } catch (WriterException e) {
      e.printStackTrace();
    }
    return bitmap;
  }

  /**
   * Generate to text below bar code.
   *
   * @param content text content
   * @param width width of text
   * @param height height of text
   * @param context context to use
   * @return text bitmap
   */
  private static Bitmap getCodeBitmap(String content, int width, int height, Context context) {
    TextView textView = new TextView(context);
    LinearLayout.LayoutParams layoutParams =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
    textView.setLayoutParams(layoutParams);
    textView.setText(content, TextView.BufferType.NORMAL);
    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    textView.setHeight(height);
    textView.setGravity(Gravity.CENTER);
    textView.setWidth(width);
    textView.setDrawingCacheEnabled(true);
    textView.setTextColor(Color.BLACK);
    textView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
    textView.buildDrawingCache();
    return textView.getDrawingCache();
  }

  /**
   * Merge two bitmap to one.
   *
   * @param first the first bitmap
   * @param second the second bitmap
   * @param fromPoint the start ponit of second bitmap
   */
  private static Bitmap mergeBitmap(Bitmap first, Bitmap second, PointF fromPoint) {
    if (first == null || second == null || fromPoint == null) {
      return null;
    }
    int marginW = 20;
    Bitmap newBitmap =
        Bitmap.createBitmap(first.getWidth() + marginW, first.getHeight() + second.getHeight(),
            Bitmap.Config.ARGB_8888);
    Canvas cv = new Canvas(newBitmap);
    cv.drawBitmap(first, marginW, 0, null);
    cv.drawBitmap(second, fromPoint.x, fromPoint.y, null);
    cv.save(Canvas.ALL_SAVE_FLAG);
    cv.restore();
    return newBitmap;
  }

  /**
   * Create a new QR bitmap.
   *
   * @param content the string to encode
   * @param widthAndHeight the width an height of the code
   * @return the bitmap
   * @throws WriterException
   */
  public static Bitmap createQRCodeBitmap(String content, int widthAndHeight)
      throws WriterException {
    BitMatrix matrix =
        new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, widthAndHeight,
            widthAndHeight, HINTS);
    int width = matrix.getWidth();
    int height = matrix.getHeight();
    int[] pixels = new int[width * height];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (matrix.get(x, y)) {
          pixels[y * width + x] = Color.BLACK;
        }
      }
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

    return bitmap;
  }
}
