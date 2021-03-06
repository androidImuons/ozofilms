package com.example.oops.activity;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.example.oops.EntityClass.LoginEntity;
import com.example.oops.EntityClass.SupportHelpEntity;
import com.example.oops.R;
import com.example.oops.ResponseClass.LogoutResponse;
import com.example.oops.ResponseClass.RegistrationResponse;
import com.example.oops.Utils.AppCommon;
import com.example.oops.Utils.ViewUtils;
import com.example.oops.retrofit.AppService;
import com.example.oops.retrofit.ServiceGenerator;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Support_Help extends AppCompatActivity {
    @BindView(R.id.ll_support_help)
    LinearLayout ll_support_help;
    @BindView(R.id.txtHeading)
    AppCompatTextView txtHeading;
    @BindView(R.id.editTextFullName)
    AppCompatEditText editTextFullName;
    String sFullName;
    @BindView(R.id.editTextMobile)
    AppCompatEditText editTextMobile;
    String sMobileNumber;
    @BindView(R.id.editTexEmail)
    AppCompatEditText editTexEmail;
    String sEmail;
    @BindView(R.id.editTextMessage)
    AppCompatEditText editTextMessage;
    String  sMessage;
    @BindView(R.id.imgBackPressed)
    AppCompatImageView imgBackPressed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.support_help);
        ButterKnife.bind(this);
        txtHeading.setText(getString(R.string.support_help));
        imgBackPressed.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.imgBackPressed)
    public  void setImgBackPressed(){
        onBackPressed();
    }

    public void support(View view){
        sFullName = editTextFullName.getText().toString().trim();
        sMobileNumber = editTextMobile.getText().toString().trim();
        sMessage = editTextMessage.getText().toString().trim();
        sEmail = editTexEmail.getText().toString().trim();

        if(sFullName.isEmpty())
            editTextFullName.setText("Please enter name");
       else  if (sMobileNumber.isEmpty())
            editTextMobile.setText("Please enter phone number");
       else if(sMessage.isEmpty())
           editTextMessage.setText("Please enter message");
       else
            callApi(sFullName , sMobileNumber,sEmail,sMessage);

    }

    private void callApi(String name, String mobile,String email,String message) {
        if (AppCommon.getInstance(this).isConnectingToInternet(this)) {
            final Dialog dialog = ViewUtils.getProgressBar(Support_Help.this);
            AppCommon.getInstance(this).setNonTouchableFlags(this);
            AppService apiService = ServiceGenerator.createService(AppService.class);
//            Change
            Call call = apiService.supportUser(new SupportHelpEntity( AppCommon.getInstance(this).getId(),AppCommon.getInstance(this).getUserId(),name, mobile,email,message));
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    AppCommon.getInstance(Support_Help.this).clearNonTouchableFlags(Support_Help.this);
                    dialog.dismiss();
                    LogoutResponse authResponse = (LogoutResponse) response.body();
                    if (authResponse != null) {
                        Log.i("Response::", new Gson().toJson(authResponse));
                        if (authResponse.getCode() == 200) {
                            showDailog(authResponse.getMessage());
                            showSnackbar(ll_support_help,authResponse.getMessage(),Snackbar.LENGTH_SHORT);

                            // startActivity(new Intent(Login.this, .class));
                            // callLoginApi(new LoginEntity(authResponse.getData().getUserId(), authResponse.getData().getPassword() , fireBase));
                        } else {
                            showSnackbar(ll_support_help,authResponse.getMessage(),Snackbar.LENGTH_SHORT);
                        }
                    } else {
                        AppCommon.getInstance(Support_Help.this).showDialog(Support_Help.this, "Server Error");
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    dialog.dismiss();
                    AppCommon.getInstance(Support_Help.this).clearNonTouchableFlags(Support_Help.this);
                    showSnackbar(ll_support_help,getResources().getString(R.string.ServerError),Snackbar.LENGTH_SHORT);
                }
            });


        } else {
            showSnackbar(ll_support_help,getResources().getString(R.string.NoInternet),Snackbar.LENGTH_SHORT);
        }
    }

    private void showDailog(String message) {
        final Dialog dialog = ViewUtils.popUp(this , true);
        TextView okBtn = dialog.findViewById(R.id.ok_button);
        TextView cancel = dialog.findViewById(R.id.cancel_button);
        TextView textMsg = dialog.findViewById(R.id.textMsg);
        cancel.setVisibility(View.GONE);
        textMsg.setText(message);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                onBackPressed();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    public void showSnackbar(View view, String message, int duration) {
        Snackbar snackbar = Snackbar.make(view, message, duration);
        snackbar.setActionTextColor(Color.WHITE);
        snackbar.setBackgroundTint(getResources().getColor(R.color.colorPrimaryDark));
        snackbar.show();
    }

}
