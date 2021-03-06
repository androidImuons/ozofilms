package com.example.oops.activity;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.example.oops.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LegalActivity extends AppCompatActivity {
    @BindView(R.id.txtHeading)
    AppCompatTextView txtHeading;
    @BindView(R.id.imgBackPressed)
    AppCompatImageView imgBackPressed;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legal_activity);
        ButterKnife.bind(this);
        txtHeading.setText(getString(R.string.legal));
        imgBackPressed.setVisibility(View.VISIBLE);

    }
    @OnClick(R.id.imgBackPressed)
    public  void setImgBackPressed(){
        onBackPressed();
    }



}
