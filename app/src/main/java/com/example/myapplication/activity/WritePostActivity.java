package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.example.myapplication.R;
import com.example.myapplication.PostInfo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

import static com.example.myapplication.Util.showToast;

public class WritePostActivity extends BasicActivity {
    private FirebaseUser user;
    private FirebaseFirestore db;
    private RelativeLayout loaderLayout;
    private PostInfo postInfo;

    private TextView textView_nickname;
    private TextView textView_telephone;
    private TextView textView_address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_post);

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        findViewById(R.id.button_send).setOnClickListener(onClickListener);
        findViewById(R.id.button_cancel).setOnClickListener(onClickListener);
        findViewById(R.id.button_editprofile).setOnClickListener(onClickListener);

        textView_nickname = (TextView) findViewById(R.id.textView_nickname) ;
        textView_telephone = (TextView) findViewById(R.id.textView_telephone) ;
        textView_address = (TextView) findViewById(R.id.textView_address) ;

        loaderLayout = findViewById(R.id.loaderLayout);

        postInfo = (PostInfo)getIntent().getSerializableExtra("postInfo");
        postInit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        userInit();
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_send:  // 입수등록 버튼 클릭했을때 동작
                    post_add();
                    break;

                case R.id.button_cancel:  // 등록취소 버튼 클릭했을때 동작
                    finish();
                    break;

                case R.id.button_editprofile:  // 개인정보수정 버튼 클릭했을때 동작
                    myStartActivity(profileActivity.class);
                    break;
            }
        }
    };

    private void post_add() {
        final String title = ((EditText) findViewById(R.id.textView_title)).getText().toString();            // 물품제목
        String p = ((EditText) findViewById(R.id.textView_price)).getText().toString();
        final int price = Integer.parseInt(p);                                                               // 가격
        final String term = ((EditText) findViewById(R.id.textView_term)).getText().toString();              // 기간
        final String contents = ((EditText) findViewById(R.id.textView_contents)).getText().toString();      // 내용
        int viewCount = 0;

        if (title.length() > 0 && price >= 0 && term.length() > 0) {
            loaderLayout.setVisibility(View.VISIBLE);
            user = FirebaseAuth.getInstance().getCurrentUser();
            db = FirebaseFirestore.getInstance();

            final DocumentReference documentReference = postInfo == null ? db.collection("posts").document() : db.collection("posts").document(postInfo.getId());
            //final Date date = postInfo == null ? new Date() : postInfo.getCreatedAt();

            // Firebase db에 정보들을 저장하기 위해 값들을 받아옴
            if (contents.length() == 0) {
                postInfo.setContents("내용없음");
            }

            if(postInfo != null) {
                viewCount = postInfo.getViewCount();
            }

            // db 저장 함수
            uploader(documentReference, new PostInfo(title, price, term, contents, user.getUid(), new Date(), viewCount));
        } else {
            showToast(WritePostActivity.this, "정보를 입력해 주세요");
        }
    }

    private void uploader(DocumentReference documentReference, final PostInfo postInfo) {
        documentReference.set(postInfo.getPostInfo())
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    loaderLayout.setVisibility(View.GONE);
                    showToast(WritePostActivity.this,"게시글 등록에 성공하셨습니다");
                    // 게시판 새로 갱신
                    Intent intent = new Intent();
                    intent.putExtra("postInfo", postInfo);
                    setResult(0, intent);
                    finish();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    loaderLayout.setVisibility(View.GONE);
                    showToast(WritePostActivity.this,"게시글 등록에 실패하셨습니다");
                }
            });
    }

    private void postInit() {
        if(postInfo != null) {
            ((EditText) findViewById(R.id.textView_title)).setText(postInfo.getTitle());
            ((EditText) findViewById(R.id.textView_price)).setText(String.valueOf(postInfo.getPrice()));
            ((EditText) findViewById(R.id.textView_term)).setText(postInfo.getTerm());
            ((EditText) findViewById(R.id.textView_contents)).setText(postInfo.getContents());
        }
    }

    private void userInit() {
        if(user != null) {
            DocumentReference documentReference = db.collection("users").document(user.getUid());
            documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null) {
                            if (document.exists()) {
                                textView_nickname.setText(document.getData().get("nickname").toString());
                                textView_telephone.setText(document.getData().get("telephone").toString());
                                //String formattingNumber = PhoneNumberUtils.formatNumber("010123454567");
                                textView_address.setText(document.getData().get("address").toString());
                            }
                        }
                    }
                }
            });
        }
        else {
            showToast(WritePostActivity.this, "잘못된 접근입니다.");
            finish();
        }
    }

    private void myStartActivity(Class c) {
        Intent intent = new Intent(this, c);
        startActivity(intent);
    }
}
