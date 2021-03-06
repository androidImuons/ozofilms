package com.example.oops.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.oops.EntityClass.LoginEntity;
import com.example.oops.EntityClass.OTPEntity;
import com.example.oops.EntityClass.RegistrationEntity;
import com.example.oops.R;
import com.example.oops.ResponseClass.ForgotPassResponse;
import com.example.oops.ResponseClass.RegistrationResponse;
import com.example.oops.ResponseClass.SocialResponse;
import com.example.oops.Utils.AppCommon;
import com.example.oops.Utils.ViewUtils;
import com.example.oops.retrofit.AppService;
import com.example.oops.retrofit.ServiceGenerator;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.Gson;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.provider.ContactsContract.Intents.Insert.EMAIL;
import static com.google.android.exoplayer2.ExoPlayerLibraryInfo.TAG;

public class Login extends Activity {

    @BindView(R.id.ll_login)
    LinearLayout ll_login;

    @BindView(R.id.editTextUserName)
    EditText editTextUserName;

    @BindView(R.id.editTextPassWord)
    EditText editTextPassWord;

    @BindView(R.id.sign_in_button)
    ImageView signInButton;

    String msgPrint;
    GoogleSignInClient mGoogleSignInClient;
    private int RC_SIGN_IN = 501;
    CallbackManager callbackManager;

    @BindView(R.id.login_button)
    LoginButton login_button;
    String fireBase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        ButterKnife.bind(this);

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.i("getInstanceId failed::", task.getException().toString());
                            return;
                        }

                        // Get new Instance ID token
                        fireBase = task.getResult().getToken();

                        // Log and toast
                        // String msg = getString(R.string.msg_token_fmt, token);
                        Log.i("token::", fireBase);
                        //   Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });


        //login_button.setReadPermissions(Arrays.asList(EMAIL));

        try {
            PackageInfo info = this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String hashKey = new String(Base64.encode(md.digest(), 0));
                Log.i(TAG, "printHashKey() Hash Key: " + hashKey);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "printHashKey()", e);
        } catch (Exception e) {
            Log.e(TAG, "printHashKey()", e);
        }
    }




    private void callSocialApi(String personName, String personEmail, String personId) {
        if (AppCommon.getInstance(this).isConnectingToInternet(this)) {
            final Dialog dialog = ViewUtils.getProgressBar(Login.this);
            AppCommon.getInstance(this).setNonTouchableFlags(this);
            AppService apiService = ServiceGenerator.createService(AppService.class);
            Map<String, String> entityMap = new HashMap<>();
            entityMap.put("name", personName);
            entityMap.put("email", personEmail);
            entityMap.put("phone", "");
            entityMap.put("subscriptionType", "free");
            entityMap.put("countryName", "India");
            entityMap.put("socialID", personId);
            entityMap.put("socialAccount", "Gmail");
            entityMap.put("deviceId",fireBase);
            Call call = apiService.socialLogin(entityMap);
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    AppCommon.getInstance(Login.this).clearNonTouchableFlags(Login.this);
                    dialog.dismiss();
                    SocialResponse authResponse = (SocialResponse) response.body();

                    if (authResponse != null) {
                        Log.i("Response::", new Gson().toJson(authResponse));
                        if (authResponse.getSuccess() == 200) {

                            if (authResponse.getData().getPin().equals("")) {
                                startActivityForResult(new Intent(Login.this, EnterPin.class)
                                                .putExtra("data", new Gson().toJson(authResponse.getData()))
                                        , 404);
                            } else {
                                AppCommon.getInstance(Login.this).setToken(authResponse.getData().getToken());
                                AppCommon.getInstance(Login.this).setUserLogin(authResponse.getData().getUserId(), true);
                                AppCommon.getInstance(Login.this).setId(authResponse.getData().getId());
                                AppCommon.getInstance(Login.this).setSocial(true, "Gmail");
                                AppCommon.getInstance(Login.this).setUserObject(new Gson().toJson(authResponse.getData()));
                                startActivity(new Intent(Login.this, Dashboard.class));
                                finishAffinity();
                            }
                            //  callInsertPin();
                            // callLoginApi(new LoginEntity(authResponse.getData().getUserId(), authResponse.getData().getPassword() , fireBase));
                        } else {
                            showSnackbar(ll_login,authResponse.getMsg(),Snackbar.LENGTH_SHORT);
                        }
                    } else {
                        //AppCommon.getInstance(Login.this).showDialog(Login.this, authResponse.getMsg());
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    dialog.dismiss();
                    AppCommon.getInstance(Login.this).clearNonTouchableFlags(Login.this);
                    showSnackbar(ll_login,getResources().getString(R.string.ServerError),Snackbar.LENGTH_SHORT);
                }
            });
        } else {
            showSnackbar(ll_login,getResources().getString(R.string.NoInternet),Snackbar.LENGTH_SHORT);
        }
    }

    public void register(View view) {
        startActivity(new Intent(this, UserRegistration.class));
    }

    private void callApi(String email, String password) {
        if (AppCommon.getInstance(this).isConnectingToInternet(this)) {
            final Dialog dialog = ViewUtils.getProgressBar(Login.this);
            AppCommon.getInstance(this).setNonTouchableFlags(this);
            AppService apiService = ServiceGenerator.createService(AppService.class);
            Call<RegistrationResponse> call = apiService.LoginApi(new LoginEntity(email, password,fireBase));
            call.enqueue(new Callback<RegistrationResponse>() {
                @Override
                public void onResponse(Call<RegistrationResponse> call, Response<RegistrationResponse> response) {
                    AppCommon.getInstance(Login.this).clearNonTouchableFlags(Login.this);
                    dialog.dismiss();
                    RegistrationResponse authResponse = response.body();

                    if (authResponse != null) {
                        Log.i("Response::", new Gson().toJson(authResponse));
                        if (authResponse.getSuccess() == 200) {
                            if(authResponse.getMsg().equals("OTP send on mobile number plz verify new device")){
                                showSnackbar(ll_login,authResponse.getMsg(),Snackbar.LENGTH_SHORT);
                                OTPEntity otpEntity = new OTPEntity(authResponse.getData().getId() , authResponse.getData().getUserId() , "",fireBase);
                                startActivity(new Intent(Login.this, OTPActivity.class)
                                                .putExtra("data", new Gson().toJson(otpEntity))
                                        );
                            }else {
                                AppCommon.getInstance(Login.this).setToken(authResponse.getData().getToken());
                                AppCommon.getInstance(Login.this).setUserLogin(authResponse.getData().getUserId(), true);
                                AppCommon.getInstance(Login.this).setId(authResponse.getData().getId());
                                AppCommon.getInstance(Login.this).setUserObject(new Gson().toJson(authResponse.getData()));
                                AppCommon.getInstance(Login.this).setDeviceId(fireBase);
                                startActivity(new Intent(Login.this, Dashboard.class));
                                finishAffinity();
                            }

                            // callLoginApi(new LoginEntity(authResponse.getData().getUserId(), authResponse.getData().getPassword() , fireBase));
                        } else {
                            showSnackbar(ll_login,authResponse.getMsg(),Snackbar.LENGTH_SHORT);
                        }
                    } else {
                        AppCommon.getInstance(Login.this).showDialog(Login.this, authResponse.getMsg());
                    }
                }

                @Override
                public void onFailure(Call<RegistrationResponse> call, Throwable t) {
                    dialog.dismiss();
                    AppCommon.getInstance(Login.this).clearNonTouchableFlags(Login.this);
                    showSnackbar(ll_login,getResources().getString(R.string.ServerError),Snackbar.LENGTH_SHORT);
                }
            });


        } else {
            showSnackbar(ll_login,getResources().getString(R.string.NoInternet),Snackbar.LENGTH_SHORT);

        }
    }

    public void login(View view) {
        String emailId = editTextUserName.getText().toString().trim();
        String pass = editTextPassWord.getText().toString().trim();
        if (emailId.isEmpty())
            editTextUserName.setError("Please enter user id");
        else if (pass.isEmpty())
            editTextPassWord.setError("Please enter password");
        else
            callApi(emailId, pass);
    }

    public void forgotPass(View view) {
        String emailName = editTextUserName.getText().toString().trim();
        if (emailName.isEmpty())
            editTextUserName.setError("Please enter email id");
        else if (!isEmailValid(emailName))
            editTextUserName.setError("Email Id is Invalid");
        else {
            callforgotApi(emailName);
        }


    }




    private void callforgotApi(String emailName) {
        if (AppCommon.getInstance(this).isConnectingToInternet(this)) {
            final Dialog dialog = ViewUtils.getProgressBar(Login.this);
            AppCommon.getInstance(this).setNonTouchableFlags(this);
            AppService apiService = ServiceGenerator.createService(AppService.class);
            Map<String, String> entityMap = new HashMap<>();
            entityMap.put("email", emailName);
            Call call = apiService.forgotPassword(entityMap);
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    AppCommon.getInstance(Login.this).clearNonTouchableFlags(Login.this);
                    dialog.dismiss();
                    ForgotPassResponse authResponse = (ForgotPassResponse) response.body();
                    if (authResponse != null) {
                        Log.i("Response::", new Gson().toJson(authResponse));
                        if (authResponse.getCode() == 200) {
                            showSnackbar(ll_login,authResponse.getMessage(),Snackbar.LENGTH_SHORT);
                            startActivity(new Intent(Login.this, ForgotPassword.class)
                                    .putExtra("isPassword", true)
                                    .putExtra("data", new Gson().toJson(authResponse.getData())));
                        } else {
                            showSnackbar(ll_login,authResponse.getMessage(),Snackbar.LENGTH_SHORT);
                        }
                    } else {
                        AppCommon.getInstance(Login.this).showDialog(Login.this, authResponse.getMessage());
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    dialog.dismiss();
                    AppCommon.getInstance(Login.this).clearNonTouchableFlags(Login.this);

                    showSnackbar(ll_login,getResources().getString(R.string.ServerError),Snackbar.LENGTH_SHORT);
                }
            });


        } else {
            showSnackbar(ll_login,getResources().getString(R.string.NoInternet),Snackbar.LENGTH_SHORT);

        }
    }

    boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }



    public void showSnackbar(View view, String message, int duration) {
        Snackbar snackbar = Snackbar.make(view, message, duration);
        snackbar.setActionTextColor(Color.WHITE);
        snackbar.setBackgroundTint(getResources().getColor(R.color.colorPrimaryDark));
        snackbar.show();
    }
}
