package com.example.a16031940.tvac;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.view.View.INVISIBLE;

public class SetUpActivity extends AppCompatActivity {

    CircleImageView setUpImage;
    Uri mainImageURI = null;
    EditText setUpName;
    Button setUpButton;
    ProgressBar setupProgress;
    Map<String, String> userMap = new HashMap<>();
    StorageReference storageReference;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    String imageURL ;

    String username , user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up);

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        user_id = firebaseAuth.getCurrentUser().getUid();

        storageReference = FirebaseStorage.getInstance().getReference();
        Toolbar setUpToolBar = findViewById(R.id.setupToolbar);
        setSupportActionBar(setUpToolBar);
        getSupportActionBar().setTitle("Account Setup");
        setUpName = findViewById(R.id.setup_name);
        setUpButton = findViewById(R.id.setup_btn);
        setupProgress = findViewById(R.id.setup_progress);
        setUpImage = findViewById(R.id.setup_image);

        setUpButton.setEnabled(false);

        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
              if(task.isSuccessful()){

                  if(task.getResult().exists()){
                      String name = task.getResult().getString("name");
                      String image = task.getResult().getString("image");

                      setUpName.setText(name);
                      mainImageURI = Uri.parse(image);

                      RequestOptions placeholderRequest = new RequestOptions();
                      placeholderRequest.placeholder(R.drawable.defaultprofile);

                      Toast.makeText(SetUpActivity.this,image,Toast.LENGTH_SHORT).show();
                      Glide.with(SetUpActivity.this).setDefaultRequestOptions(placeholderRequest).load(image).into(setUpImage);
                      Log.d("uploadimgea",image);
                  }
              }else{
                  Toast.makeText(SetUpActivity.this,"Firestore retrieve error : " + task.getException().getMessage().toString(),Toast.LENGTH_SHORT).show();
              }

                setUpButton.setEnabled(true);

            }
        });

        setUpImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if(ContextCompat.checkSelfPermission(SetUpActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                        Toast.makeText(SetUpActivity.this,"Permission Denied!",Toast.LENGTH_SHORT).show();

                        ActivityCompat.requestPermissions(SetUpActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

                    }else{


                        BringImagePicker();

                    }
                }else{

                    BringImagePicker();

                }

            }
        });

        setUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final String username = setUpName.getText().toString();
                if(!TextUtils.isEmpty(username) && mainImageURI != null){
                    user_id = firebaseAuth.getCurrentUser().getUid();
                    setupProgress.setVisibility(View.VISIBLE);
                    final StorageReference image_path = storageReference.child("profile_images").child(user_id + ".jpg");

                    image_path.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // Got the download URL for 'users/me/profile.png'
                            imageURL = uri.toString();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle any errors
                            Toast.makeText(SetUpActivity.this,"Error is " + exception.getMessage().toString(),Toast.LENGTH_SHORT).show();
                        }
                    });

                    image_path.putFile(mainImageURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(Task<UploadTask.TaskSnapshot> task) {

                            if(task.isSuccessful()){

                                storeFireStore(task,username);

                            }else{
                                String error = task.getException().getMessage();
                                Toast.makeText(SetUpActivity.this,"Image Error: " + error,Toast.LENGTH_SHORT).show();
                                setupProgress.setVisibility(INVISIBLE);

                            }


                        }
                    });
                }else{

                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                 mainImageURI = result.getUri();
                 setUpImage.setImageURI(mainImageURI);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(SetUpActivity.this,error.getMessage(),Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void BringImagePicker(){
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(SetUpActivity.this);
    }


    private void storeFireStore(Task<UploadTask.TaskSnapshot> task , String user_name){
        // Uri download_uri = task.getResult().getUploadSessionUri();
//                                Toast.makeText(SetUpActivity.this,"Image updated",Toast.LENGTH_SHORT).show();
        userMap.put("name",username);
        userMap.put("image",imageURL);

        firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(task.isSuccessful()){

                    Toast.makeText(SetUpActivity.this,"User settings updated!",Toast.LENGTH_SHORT).show();
                    Intent mainIntent = new Intent(SetUpActivity.this,MainActivity.class);
                    startActivity(mainIntent);
                    finish();

                }else{

                    String error = task.getException().getMessage();
                    Toast.makeText(SetUpActivity.this,"FireStore error: " + error,Toast.LENGTH_SHORT).show();

                }
                setupProgress.setVisibility(View.INVISIBLE);


            }
        });
    }

}
