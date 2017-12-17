package com.anbillon.barcodescanview.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.anbillon.barcodescanview.BarCodeScanView;
import com.google.zxing.Result;

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class CaptureActivity extends AppCompatActivity
    implements BarCodeScanView.OnBarCodeReadListener {
  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    BarCodeScanView barCodeScanView = new BarCodeScanView(this);
    setContentView(barCodeScanView);
    barCodeScanView.setOnBarCodeReadListener(this);
  }

  @Override public void onBarCodeRead(Result result) {
    Log.d("TAG", "result  >>>>>>>: " + result.getText());
  }
}
