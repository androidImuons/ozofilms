package com.example.oops.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.example.oops.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VideoQuality extends AppCompatActivity {
    @BindView(R.id.txtHeading)
    AppCompatTextView txtHeading;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_quality);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
        txtHeading.setText(getString(R.string.video_quality));
    }
}
