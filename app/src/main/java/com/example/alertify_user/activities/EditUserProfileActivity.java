package com.example.alertify_user.activities;

import static com.example.alertify_user.constants.Constants.USERS_REF;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.alertify_user.R;
import com.example.alertify_user.databinding.ActivityEditUserProfileBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class EditUserProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private Dialog userUpdateNameDialog, userUpdatePasswordDialog;

    private ProgressBar userNameUpdateDialogProgressBar, userPasswordUpdateDialogProgressBar;

    private DatabaseReference userRef;

    private FirebaseUser firebaseUser;

    private EditText dialogUserName;

    private TextInputEditText userCurrentPassword, userNewPassword;

    private SharedPreferences userData;

    private SharedPreferences.Editor editor;

    private ActivityEditUserProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
    }

    private void init() {
        userData = getSharedPreferences("userData", MODE_PRIVATE);
        editor = userData.edit();

        binding.nameEditBtn.setOnClickListener(this);
        binding.passwordEditBtn.setOnClickListener(this);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        userRef = FirebaseDatabase.getInstance().getReference(USERS_REF);

        getProfileData(); // set method for load user data to the profile
    }

    private void getProfileData() {
        binding.userName.setText(userData.getString("name", ""));
        binding.userEmail.setText(userData.getString("email", ""));
        binding.userCnic.setText(userData.getString("cnicNo", ""));
        binding.userPhone.setText(userData.getString("phoneNo", ""));
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.nameEditBtn:
                createUserNameDialog();
                break;
            case R.id.passwordEditBtn:
                createUserPasswordDialog();
                break;
        }
    }

    private void createUserNameDialog() {
        userUpdateNameDialog = new Dialog(EditUserProfileActivity.this);
        userUpdateNameDialog.setContentView(R.layout.user_edit_name_dialog);
        userUpdateNameDialog.show();
        userUpdateNameDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        userNameUpdateDialogProgressBar = userUpdateNameDialog.findViewById(R.id.user_name_progressbar);

        dialogUserName = userUpdateNameDialog.findViewById(R.id.user_dialog_name);
        dialogUserName.setText(userData.getString("name", ""));

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

        HashMap<String, Object> map = new HashMap<>();

        map.put("name", updatedName);

        userRef.child(firebaseUser.getUid()).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    userNameUpdateDialogProgressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(EditUserProfileActivity.this, "User Name Updated Successfully!", Toast.LENGTH_SHORT).show();
                    userUpdateNameDialog.dismiss();
                    binding.userName.setText(updatedName);
                    editor.putString("name", updatedName);
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
