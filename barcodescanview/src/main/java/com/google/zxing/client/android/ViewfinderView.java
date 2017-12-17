package com.google.zxing.client.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import com.anbillon.barcodescanview.R;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;
import java.util.ArrayList;
import java.util.List;

/**
 * The view finder view.
 */
public final class ViewfinderView extends View {
  /* the alpha of the scan page */
  private static final int[] SCANNER_ALPHA = { 0, 64, 128, 192, 255, 192, 128, 64 };
  private static final long ANIMATION_DELAY = 28L;
  private static final int CURRENT_POINT_OPACITY = 0xA0;
  private static final int MAX_RESULT_POINTS = 20;
  private static final int POINT_SIZE = 10;
  private static final int LINE_MOVE_DOWN = 0;
  private static final int LINE_MOVE_UP = 1;
  private static final int CORNER_LINE_WITH = 6;

  private CameraManager cameraManager;
  private final Paint paint;
  private final TextPaint labelPaint;
  /* the result bitmap */
  private Bitmap resultBitmap;
  private final int frameColor;
  private final int maskColor;
  private final int resultColor;
  /* the color of laser line in the rect */
  private final int laserLineColor;
  /* the color of the result point(flash on the screen) */
  private final int resultPointColor;
  /* the alpha of the scanner page */
  private int scannerAlpha;
  private List<ResultPoint> possibleResultPoints;
  private List<ResultPoint> lastPossibleResultPoints;

  private int linePosition = 0;
  private int cornerLinewidth = CORNER_LINE_WITH;
  private int lineMoveDirection = LINE_MOVE_DOWN;
  private String label;

  /* This constructor is used when the class is built from an XML resource. */
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);
    /* initialize these once for performance rather than calling them every time in onDraw() */
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    labelPaint.setColor(Color.WHITE);
    labelPaint.setTextSize(40);
    labelPaint.setTextAlign(Paint.Align.CENTER);

    frameColor = getColor(context, R.color.viewfinder_frame);
    maskColor = getColor(context, R.color.viewfinder_mask);
    resultColor = getColor(context, R.color.result_view);
    laserLineColor = getColor(context, R.color.viewfinder_laser);
    resultPointColor = getColor(context, R.color.possible_result_points);
    scannerAlpha = 0;
    possibleResultPoints = allocateResultPoint(5);
    lastPossibleResultPoints = null;
    label = context.getString(R.string.default_label);
  }

  @Override public void onDraw(Canvas canvas) {
    if (cameraManager == null) {
      /* not ready yet, early draw before done configuring */
      return;
    }

		/* get the framing rect */
    Rect frame = cameraManager.getFramingRect();
    if (frame == null) {
      return;
    }

    if (linePosition == 0) {
      /* initialize the line to the top of frame */
      linePosition = frame.top;
    }

    int width = canvas.getWidth();
    int height = canvas.getHeight();
    // Draw the exterior (i.e. outside the framing rect) darkened
    paint.setColor(resultBitmap != null ? resultColor : maskColor);
    /* the top of rect */
    canvas.drawRect(0, 0, width, frame.top, paint);
    /* the left of rect */
    canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
    /* the right of rect */
    canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
    /* the buttom of rect */
    canvas.drawRect(0, frame.bottom + 1, width, height, paint);

    if (resultBitmap != null) {
      /* draw the opaque result bitmap over the scanning rectangle */
      paint.setAlpha(CURRENT_POINT_OPACITY);
      canvas.drawBitmap(resultBitmap, null, frame, paint);
    } else {
      /* the width of corner line */
      paint.setColor(frameColor);
      /* draw the 4 corners of the scanner rect */
      canvas.drawRect(frame.left, frame.top, (cornerLinewidth + frame.left), (50 + frame.top),
          paint);
      canvas.drawRect(frame.left, frame.top, (50 + frame.left), (cornerLinewidth + frame.top),
          paint);
      canvas.drawRect(((0 - cornerLinewidth) + frame.right), frame.top, (1 + frame.right),
          (50 + frame.top), paint);
      canvas.drawRect((-50 + frame.right), frame.top, frame.right, (cornerLinewidth + frame.top),
          paint);
      canvas.drawRect(frame.left, (-49 + frame.bottom), (cornerLinewidth + frame.left),
          (1 + frame.bottom), paint);
      canvas.drawRect(frame.left, ((0 - cornerLinewidth) + frame.bottom), (50 + frame.left),
          (1 + frame.bottom), paint);
      canvas.drawRect(((0 - cornerLinewidth) + frame.right), (-49 + frame.bottom),
          (1 + frame.right), (1 + frame.bottom), paint);
      canvas.drawRect((-50 + frame.right), ((0 - cornerLinewidth) + frame.bottom), frame.right,
          (cornerLinewidth - (cornerLinewidth - 1) + frame.bottom), paint);

      // Draw a red "laser scanner" line through the middle to show decoding is active
      /* draw the scanner line */
      paint.setColor(laserLineColor);
      paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
      scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;

			/* move the laser line in the scanner area */
      if (lineMoveDirection == LINE_MOVE_DOWN) {
        linePosition += 10;
        if (linePosition >= frame.bottom - cornerLinewidth) lineMoveDirection = LINE_MOVE_UP;
      } else if (lineMoveDirection == LINE_MOVE_UP) {
        linePosition -= 10;
        if (linePosition <= frame.top + cornerLinewidth) lineMoveDirection = LINE_MOVE_DOWN;
      }

			/* modify the laser line */
      canvas.drawRect(frame.left + cornerLinewidth, linePosition - 1, frame.right - cornerLinewidth,
          linePosition + 3, paint);

			/* prepare the frame rect */
      Rect previewFrame = cameraManager.getFramingRectInPreview();
      float scaleX = frame.width() / (float) previewFrame.width();
      float scaleY = frame.height() / (float) previewFrame.height();
      List<ResultPoint> currentPossible = possibleResultPoints;
      List<ResultPoint> currentLast = lastPossibleResultPoints;
      int frameLeft = frame.left;
      int frameTop = frame.top;
      if (currentPossible.isEmpty()) {
        lastPossibleResultPoints = null;
      } else {
        possibleResultPoints = allocateResultPoint(5);
        lastPossibleResultPoints = currentPossible;
        paint.setAlpha(CURRENT_POINT_OPACITY);
        paint.setColor(resultPointColor);
        synchronized (currentPossible) {
          for (ResultPoint point : currentPossible) {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                frameTop + (int) (point.getY() * scaleY), POINT_SIZE, paint);
          }
        }
      }

      if (currentLast != null) {
        paint.setAlpha(CURRENT_POINT_OPACITY / 2);
        paint.setColor(resultPointColor);
        synchronized (currentLast) {
          float radius = POINT_SIZE / 2.0f;
          for (ResultPoint point : currentLast) {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                frameTop + (int) (point.getY() * scaleY), radius, paint);
          }
        }
      }
      // Request another update at the animation interval, but only repaint the laser line,
      // not the entire viewfinder mask.
      postInvalidateDelayed(ANIMATION_DELAY, frame.left - POINT_SIZE, frame.top - POINT_SIZE,
          frame.right + POINT_SIZE, frame.bottom + POINT_SIZE);
    }

    canvas.drawText(label, getWidth() / 2, frame.bottom + 100, labelPaint);
  }

  /**
   * Get color for api 23 and lower.
   *
   * @param context context to use
   * @param resId color resrouce id
   * @return color int
   */
  @SuppressWarnings("deprecation") static int getColor(Context context, int resId) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return context.getColor(resId);
    } else {
      return context.getResources().getColor(resId);
    }
  }

  /**
   * Allocate the result point.
   *
   * @param num the number of result point
   * @return the list
   */
  private List<ResultPoint> allocateResultPoint(int num) {
    return new ArrayList<>(num);
  }

  /**
   * Set the camera manager.
   */
  public void setCameraManager(CameraManager cameraManager) {
    this.cameraManager = cameraManager;
  }

  /**
   * Set the width for corner line.
   *
   * @param width width
   */
  public void setCornerLineWidth(int width) {
    cornerLinewidth = width;
  }

  /**
   * Set text for label to show.
   *
   * @param label label
   */
  public void setLabelText(String label) {
    if (label == null) {
      return;
    }

    this.label = label;
  }

  /**
   * Set text for label to show.
   *
   * @param resId label resource id
   */
  public void setLabelText(int resId) {
    label = getContext().getString(resId);
  }

  /**
   * Set text size for label.
   *
   * @param textSize text size
   */
  public void setLabelTextSize(float textSize) {
    labelPaint.setTextSize(textSize);
  }

  /**
   * Draw the view finder view.
   */
  public void drawViewfinder() {
    Bitmap resultBitmap = this.resultBitmap;
    this.resultBitmap = null;
    if (resultBitmap != null) {
      resultBitmap.recycle();
    }

    invalidate();
  }

  /**
   * Draw a bitmap with the result points highlighted instead of the live
   * scanning display.
   *
   * @param barcode An image of the decoded barcode.
   */
  public void drawResultBitmap(Bitmap barcode) {
    resultBitmap = barcode;
    invalidate();
  }

  /**
   * Add the reuslt point.
   */
  public void addPossibleResultPoint(ResultPoint point) {
    List<ResultPoint> points = possibleResultPoints;
    synchronized (points) {
      points.add(point);
      int size = points.size();
      if (size > MAX_RESULT_POINTS) {
        points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
      }
    }
  }
}