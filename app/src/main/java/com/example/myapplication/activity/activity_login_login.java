package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.example.myapplication.R;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import static com.example.myapplication.util.util_util.showToast;


public class activity_login_login extends activity_main_basic {
    final int RC_SIGN_IN = 1001; // 로그인 확인여부 코드
    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient; //API 클라이언트
    private static final String TAG = "activity_login_main";
    private RelativeLayout loaderLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_main);

        findViewById(R.id.bt_sign_in).setOnClickListener(onClickListener);
        findViewById(R.id.bt_sign_up).setOnClickListener(onClickListener);
        findViewById(R.id.button_findpassword).setOnClickListener(onClickListener);
        findViewById(R.id.google_login_button).setOnClickListener(onClickListener);

        loaderLayout = findViewById(R.id.loaderLayout);

        mAuth = FirebaseAuth.getInstance(); // 인스턴스 생성

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();

                Log.d(TAG, "이름 =" + account.getDisplayName());
                Log.d(TAG, "이메일=" + account.getEmail());
                Log.d(TAG, "getId()=" + account.getId());
                Log.d(TAG, "getAccount()=" + account.getAccount());
                Log.d(TAG, "getIdToken()=" + account.getIdToken());
                firebaseAuthWithGoogle(account);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        loaderLayout.setVisibility(View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            loaderLayout.setVisibility(View.GONE);
                            myStartActivity(activity_main_main.class);
                            finish();
                        } else {
                            loaderLayout.setVisibility(View.GONE);
                            showToast(activity_login_login.this, "Authentication failed.");
                        }
                    }
                });
    }

    public void onBackPressed() {
        showToast(activity_login_login.this, "앱을 종료합니다.");
        super.onBackPressed();
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.bt_sign_in:  // 로그인 버튼 클릭했을때 동작
                    login();
                    break;

                case R.id.bt_sign_up:  // 회원가입 버튼 클릭했을때 동작
                    myStartActivity(activity_login_signUp.class);
                    break;

                case R.id.button_findpassword:  // 비밀번호 찾기 버튼 클릭했을때 동작
                    myStartActivity(activity_login_resetPassword.class);
                    break;

                case R.id.google_login_button:  // 구글로그인 버튼 클릭했을때 동작
                    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                    break;
            }
        }
    };

    private void login() {
        String email = ((EditText) findViewById(R.id.username)).getText().toString();
        String password = ((EditText) findViewById(R.id.password)).getText().toString();

        if (email.length() > 0 && password.length() > 0) {
            loaderLayout.setVisibility(View.VISIBLE);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            loaderLayout.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                showToast(activity_login_login.this, "로그인에 성공했습니다");

                                // 로그인한 사용자 정보가 이미 입력되어 있는 상태라면 MainActivity로 아니라면 addInfo로 이동
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                if (user != null) {
                                    DocumentReference docRef = db.collection("users").document(user.getUid());
                                    docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                DocumentSnapshot document = task.getResult();
                                                if (document != null) {
                                                    if (document.exists()) {
                                                        myStartActivity(activity_main_main.class);
                                                        showToast(activity_login_login.this, "반갑습니다");
                                                    } else {
                                                        myStartActivity(activity_login_addInfo.class);
                                                        showToast(activity_login_login.this, "회원정보를 추가입력해주세요");
                                                    }
                                                }
                                            } else {
                                                Log.d(TAG, "get failed with ", task.getException());
                                            }
                                        }
                                    });
                                }
                            } else {
                                if (task.getException() != null) {
                                    showToast(activity_login_login.this, task.getException().toString());
                                }
                            }
                        }
                    });
        } else {
            showToast(activity_login_login.this, "이메일 또는 비밀번호를 입력해 주세요");
        }
    }

    private void myStartActivity(Class c) {
        Intent intent = new Intent(this, c);
        startActivity(intent);
    }
}