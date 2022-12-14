package com.example.shareourwedding;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ImageMainActivity extends AppCompatActivity {

    private ImageView imageView;
    private ProgressBar progressBar;
    private Intent intent;
    private String couple_id;

    private final DatabaseReference root = FirebaseDatabase.getInstance().getReference("SHOW").child("IMAGE");
    private final StorageReference reference = FirebaseStorage.getInstance().getReference();

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_main);

        intent = getIntent();

        couple_id = intent.getStringExtra("id");

        //컴포넌트 객체에 담기
        Button uploadBtn = findViewById(R.id.upload_btn);
        progressBar = findViewById(R.id.progress_View);
        imageView = findViewById(R.id.image_view);

        //프로그래스바 숨기기
        progressBar.setVisibility(View.INVISIBLE);

        //이미지 클릭 이벤트
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                activityResult.launch(galleryIntent);
            }
        });

        /*imageView.setOnClickListener(new View.OnClickListener() {
            ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                    new ActivityResultCallback<Uri>() {
                        @Override
                        public void onActivityResult(Uri uri) {
                            imageView.setImageURI(uri);
                        }
                    });

            public void onClick(View v) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(ImageMainActivity.this);
                dlg.setTitle("알람");
                dlg.setMessage("갤러리로 이동하시겠습니까?");
                dlg.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mGetContent.launch("image/*");
                                Intent galleryIntent = new Intent();
                                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                                galleryIntent.setType("image/*");
                                activityResult.launch(galleryIntent);

                            }
                        });

                dlg.setNegativeButton("CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });

                dlg.show();
            }
        });*/

        //업로드버튼 클릭이벤트
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(imageUri != null){
                    uploadToFirebase(imageUri);
                }else{
                    Toast.makeText(ImageMainActivity.this, "사진을 선택해주세요",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        //이미지 리스트
        Button imageListBtn = findViewById(R.id.image_list_btn);
        imageListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent id = new Intent(ImageMainActivity.this, ImageActivity.class);
                id.putExtra("id", couple_id);

                startActivity(id);
            }
        });
    }//onCreate

    //파이어베이스 이미지 업로드
    private void uploadToFirebase(Uri uri) {

        StorageReference fileRef = reference.child(System.currentTimeMillis() + "." +
                getFileExtension(uri));

        fileRef.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //성공시

                fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        //이미지 모델에 담기
                        Model model = new Model(uri.toString());

                        //키로 아이디 생성
                        String modelId = root.push().getKey();

                        //데이터 넣기
                        root.child(couple_id).child(modelId).setValue(model);

                        //프로그래스바 숨김
                        progressBar.setVisibility(View.INVISIBLE);

                        Toast.makeText(ImageMainActivity.this, "업로드 성공",
                                Toast.LENGTH_SHORT).show();

                        imageView.setImageResource(R.drawable.ic_add_photo);
                    }
                });
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                //프로그래스바 보여주기
                progressBar.setVisibility(View.VISIBLE);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //실패
                Toast.makeText(ImageMainActivity.this, "업로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getFileExtension(Uri uri){

        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        return mime.getExtensionFromMimeType(cr.getType(uri));
    }

    //사진 가져오기
    ActivityResultLauncher<Intent> activityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    if(result.getResultCode() == RESULT_OK && result.getData() != null){

                        imageUri = result.getData().getData();

                        imageView.setImageURI(imageUri);
                    }
                }
            });


}//MainActivity