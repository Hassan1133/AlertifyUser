package com.example.alertify_user.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.alertify_user.R;
import com.example.alertify_user.main_utils.LoadingDialog;
import com.example.alertify_user.models.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditUserProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private ShapeableImageView userDialogImg;

    private UserModel user;

    private String imageUrl;

    private Uri imageUri;

    private StorageReference firebaseStorageReference;

    private TextView userName, userEmail, userCnicNo, userPhoneNo;

    private CircleImageView userImage;

    private Dialog userUpdateImgDialog, userUpdateNameDialog, userUpdatePasswordDialog;

    private ProgressBar userImgUpdateDialogProgressBar, userNameUpdateDialogProgressBar, userPasswordUpdateDialogProgressBar;

    private DatabaseReference userRef;

    private FirebaseUser firebaseUser;

    private ImageView userNameEditBtn, userPasswordEditBtn;

    private EditText dialogUserName;

    private TextInputEditText userCurrentPassword, userNewPassword;

    private SharedPreferences userData;

    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_profile);
        init();
        LoadingDialog.showLoadingDialog(EditUserProfileActivity.this);
    }

    private void init() {
        userImage = findViewById(R.id.user_image);
        userImage.setOnClickListener(this);

        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);
        userCnicNo = findViewById(R.id.user_cnic);
        userPhoneNo = findViewById(R.id.user_phone);

        userNameEditBtn = findViewById(R.id.name_edit_btn);
        userNameEditBtn.setOnClickListener(this);
        userPasswordEditBtn = findViewById(R.id.password_edit_btn);
        userPasswordEditBtn.setOnClickListener(this);

        userRef = FirebaseDatabase.getInstance().getReference("AlertifyUser");

        firebaseStorageReference = FirebaseStorage.getInstance().getReference();

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        getProfileData(firebaseUser); // set method for load user data to the profile

        userData = getSharedPreferences("userData", MODE_PRIVATE);

        editor = userData.edit();
    }

    private void getProfileData(FirebaseUser firebaseUser) {
        userRef.child(firebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                user = snapshot.getValue(UserModel.class);

                if (user != null) {

                    Glide.with(getApplicationContext()).load(user.getImgUrl()).into(userImage);

                    userName.setText(user.getName());
                    userEmail.setText(user.getEmail());
                    userCnicNo.setText(user.getCnicNo());
                    userPhoneNo.setText(user.getPhoneNo());

                    LoadingDialog.hideLoadingDialog();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(EditUserProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.user_image:
                createUserImageDialog();
                break;
            case R.id.name_edit_btn:
                createUserNameDialog();
                break;
            case R.id.password_edit_btn:
                createUserPasswordDialog();
                break;
        }
    }

    private void createUserImageDialog() {
        userUpdateImgDialog = new Dialog(EditUserProfileActivity.this);
        userUpdateImgDialog.setContentView(R.layout.user_edit_img_dialog);
        userUpdateImgDialog.show();
        userUpdateImgDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        userImgUpdateDialogProgressBar = userUpdateImgDialog.findViewById(R.id.user_img_progressbar);

        userDialogImg = userUpdateImgDialog.findViewById(R.id.user_dialog_image);
        Glide.with(getApplicationContext()).load(user.getImgUrl()).into(userDialogImg);

        userDialogImg.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                pickNewImage();
            }
        });

        userUpdateImgDialog.findViewById(R.id.dep_admin_img_close_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userUpdateImgDialog.dismiss();
            }
        });

        userUpdateImgDialog.findViewById(R.id.user_img_update_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userImgUpdateDialogProgressBar.setVisibility(View.VISIBLE);
                if (imageUri == null) {
                    updateImageUrlToDb();
                } else if (imageUri != null) {
                    uploadImage();
                }
            }
        });

    }

    private void uploadImage() {
        StorageReference strRef = firebaseStorageReference.child("AlertifyUserImages/" + firebaseUser.getUid());

        strRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                strRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {

                        imageUrl = task.getResult().toString();
                        updateImageUrlToDb();
                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Toast.makeText(EditUserProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditUserProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateImageUrlToDb() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("imgUrl", imageUrl);

        userRef.child(firebaseUser.getUid()).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    userImgUpdateDialogProgressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(EditUserProfileActivity.this, "User Image Updated Successfully!", Toast.LENGTH_SHORT).show();
                    userUpdateImgDialog.dismiss();

                    user.setImgUrl(imageUrl);
                    editor.putString("imgUrl", imageUrl);
                    editor.apply();
                    Glide.with(getApplicationContext()).load(user.getImgUrl()).into(userImage);

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditUserProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickNewImage() {
        getContent.launch("image/*");
    }

    ActivityResultLauncher<String> getContent = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri result) {
            if (result != null) {
                imageUri = result;
                userDialogImg.setImageURI(imageUri);
            }
        }
    });

    private void createUserNameDialog() {
        userUpdateNameDialog = new Dialog(EditUserProfileActivity.this);
        userUpdateNameDialog.setContentView(R.layout.user_edit_name_dialog);
        userUpdateNameDialog.show();
        userUpdateNameDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        userNameUpdateDialogProgressBar = userUpdateNameDialog.findViewById(R.id.user_name_progressbar);

        dialogUserName = userUpdateNameDialog.findViewById(R.id.user_dialog_name);
        dialogUserName.setText(user.getName());

        userUpdateNameDialog.findViewById(R.id.close_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userUpdateNameDialog.dismiss();
            }
        });

        userUpdateNameDialog.findViewById(R.id.update_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogUserName.getText().length() < 3) {
                    dialogUserName.setError("Please enter valid name");
                } else {
                    userNameUpdateDialogProgressBar.setVisibility(View.VISIBLE);
                    updateNameToDb(dialogUserName.getText().toString().trim());
                }
            }
        });
    }

    private void updateNameToDb(String updatedName) {
        user.setName(updatedName);

        HashMap<String, Object> map = new HashMap<>();

        map.put("name", user.getName());

        userRef.child(firebaseUser.getUid()).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    userNameUpdateDialogProgressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(EditUserProfileActivity.this, "Department Admin Name Updated Successfully!", Toast.LENGTH_SHORT).show();
                    userUpdateNameDialog.dismiss();

                    dialogUserName.setText(user.getName());
                    editor.putString("name", user.getName());
                    editor.apply();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditUserProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createUserPasswordDialog() {
        userUpdatePasswordDialog = new Dialog(EditUserProfileActivity.this);
        userUpdatePasswordDialog.setContentView(R.layout.user_edit_password_dialog);
        userUpdatePasswordDialog.show();
        userUpdatePasswordDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        userCurrentPassword = userUpdatePasswordDialog.findViewById(R.id.user_current_password);
        userNewPassword = userUpdatePasswordDialog.findViewById(R.id.user_new_password);
        userPasswordUpdateDialogProgressBar = userUpdatePasswordDialog.findViewById(R.id.user_password_progressbar);

        userUpdatePasswordDialog.findViewById(R.id.close_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userUpdatePasswordDialog.dismiss();
            }
        });

        userUpdatePasswordDialog.findViewById(R.id.update_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isValidPassword()) {
                    userPasswordUpdateDialogProgressBar.setVisibility(View.VISIBLE);
                    verifyUserCurrentPassword(firebaseUser.getEmail(), userCurrentPassword.getText().toString().trim());
                }
            }
        });
    }

    private void verifyUserCurrentPassword(String email, String password) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateUserPassword(userNewPassword.getText().toString().trim());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        userPasswordUpdateDialogProgressBar.setVisibility(View.INVISIBLE);
                        userCurrentPassword.setError("password is invalid");
                        Toast.makeText(EditUserProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserPassword(String newPassword) {
        firebaseUser.updatePassword(newPassword)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(EditUserProfileActivity.this, "User Password Updated Successfully", Toast.LENGTH_SHORT).show();
                            userPasswordUpdateDialogProgressBar.setVisibility(View.INVISIBLE);
                            userUpdatePasswordDialog.dismiss();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        userPasswordUpdateDialogProgressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(EditUserProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isValidPassword() {
        boolean valid = true;

        if (userCurrentPassword.getText().length() < 6) {
            userCurrentPassword.setError("enter valid password");
            valid = false;
        }

        if (userNewPassword.getText().length() < 6) {
            userNewPassword.setError("enter valid password");
            valid = false;
        }

        return valid;
    }
}
