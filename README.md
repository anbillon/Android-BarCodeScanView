
BarCodeScanView
====================
Bar/QR code scan view and generator library.


Usage
=====
* Barcode scan. First, add `BarCodeScanView` in your layout:

```xml
<com.anbillon.barcodescanview.BarCodeScanView
      android:id="@+id/qr_code_scan"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      app:labelTextSize="16sp"
      />
```

Then get `BarCodeScanView` to use:

```java
public class MainActivity extends AppCompatActivity
    implements View.OnClickListener, BarCodeScanView.OnCameraErrorListener,
    BarCodeScanView.OnBarCodeReadListener {
  private BarCodeScanView barCodeScanView;

  @SuppressWarnings("ConstantConditions") @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    barCodeScanView = (BarCodeScanView) findViewById(R.id.qr_code_scan);
    /* set auto focus internal */
    barCodeScanView.setAutofocusInterval(1000L);
    barCodeScanView.setOnClickListener(this);
    /* you will receive error if failed to open camera */
    barCodeScanView.setOnCameraErrorListener(this);
    /* receive bar code scan result */
    barCodeScanView.setOnBarCodeReadListener(this);
  }

  @Override public void onClick(View v) {
    barCodeScanView.restart();
  }

  @Override public void onCameraError(String errorMsg) {
    Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
  }

  @Override public void onBarCodeRead(Result result) {
    Toast.makeText(this, "result: " + result.getText(), Toast.LENGTH_LONG).show();
  }
```

* Barcode generator:

```java
/* 1d barcode */
Bitmap 1dBitmap = Encoder.generateBarcodeBitmap("1d barcode", 100, 100);
/* 2d barcode */
Bitmap 2dBitmap = Encoder.createQRCodeBitmap("2d barcode", 100);
```

* Support attributes:


| Name                     | Description                              |
| ------------------------ | ---------------------------------------- |
| labelText                | Set the text on BarCodeScanView          |
| labelTextSize            | Set the text size of label               |
| shouldPlayBeepAndVibrate | Should play beep and vibrate when bar code is decoded |




Download
========
	compile 'com.anbillon.barcode.barcodescanview:1.0.0-SNAPSHOT'
