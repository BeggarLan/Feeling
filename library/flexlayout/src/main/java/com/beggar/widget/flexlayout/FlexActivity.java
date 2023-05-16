package com.beggar.widget.flexlayout;

import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;

/**
 * author: lanweihua
 * created on: 2023/3/29 3:47 PM
 * description:
 */
public class FlexActivity extends Activity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_flex_layout);
    FlexLayout flexLayout = findViewById(R.id.flex_container);
    Random random = new Random();
    for (int i = 0; i < 30; ++i) {
      TextView textView = new TextView(FlexActivity.this);
      textView.setTextSize(Math.max(18, random.nextInt(30)));
      textView.setText(random.nextInt(1000000000) / (i+1) + "");
      flexLayout.addView(textView);
    }
  }
}
