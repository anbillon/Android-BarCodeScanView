package com.tourbillon.barcodescanview.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    findViewById(R.id.main_btn_scan).setOnClickListener(this);
  }

  @Override public void onClick(View v) {
    startActivity(new Intent(this, CaptureActivity.class));
  }
}
