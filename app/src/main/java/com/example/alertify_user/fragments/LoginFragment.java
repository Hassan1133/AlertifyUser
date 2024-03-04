package com.example.alertify_user.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.alertify_user.R;
import com.example.alertify_user.activities.MainActivity;
import com.example.alertify_user.databinding.LoginBinding;
import com.example.alertify_user.main_utils.LoadingDialog;
import com.example.alertify_user.models.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginFragment extends Fragment implements View.OnClickListener {

    private LoginBinding binding;
    private FirebaseAuth firebaseAuth;

    private DatabaseReference userRef;

    private UserModel user;

    private Dialog loadingDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = LoginBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }

    private void init() {
        binding.loginBtn.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();

        userRef = FirebaseDatabase.getInstance().getReference("AlertifyUser");

        user = new UserModel();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.loginBtn:
                if (isValid()) {
                    loadingDialog = LoadingDialog.showLoadingDialog(getActivity());
                    checkForSignIn(binding.email.getText().toString().trim());
                }
                break;
        }
    }

    private void checkForSignIn(String emailText) {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {

            int count = 0;

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapShot : snapshot.getChildren()) {
                        UserModel userModel = userSnapShot.getValue(UserModel.class);

                        count++;

                        if (userModel.getEmail().equals(emailText) && userModel.getType().equals("user") && userModel.getUserStatus().equals("unblock")) {
                            user.setId(userModel.getId());
                            user.setEmail(userModel.getEmail());
                            user.setName(userModel.getName());
                            user.setPhoneNo(userModel.getPhoneNo());
                            user.setCnicNo(userModel.getCnicNo());
                            user.setImgUrl(userModel.getImgUrl());
                            signIn();
                            return;
                        } else if (count == snapshot.getChildrenCount()) {
                            LoadingDialog.hideLoadingDialog(loadingDialog);
                            Toast.makeText(getActivity(), "Account doesn't exist", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    LoadingDialog.hideLoadingDialog(loadingDialog);
                    Toast.makeText(getActivity(), "Account doesn't exist", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void signIn() {
        firebaseAuth
                .signInWithEmailAndPassword(binding.email.getText().toString().trim(), binding.password.getText().toString().trim())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            getProfileData();
                            LoadingDialog.hideLoadingDialog(loadingDialog);
                            Toast.makeText(getContext(), "Logged in Successfully", Toast.LENGTH_SHORT).show();
                            goToMainActivity();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        LoadingDialog.hideLoadingDialog(loadingDialog);
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(getContext(), "The Password is wrong", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean isValid() // method for data validation
    {
        boolean valid = true;

        if (!Patterns.EMAIL_ADDRESS.matcher(binding.email.getText()).matches()) {
            binding.email.setError("enter valid email");
            valid = false;
        }
        if (binding.password.getText().length() < 6) {
            binding.password.setError("enter valid name");
            valid = false;
        }

        return valid;
    }

    private void goToMainActivity() {
        SharedPreferences pref = getActivity().getSharedPreferences("login", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("flag", true);
        editor.apply();

        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
        getActivity().finish();
    }

    private void getProfileData() {
        SharedPreferences userData = getContext().getSharedPreferences("userData", MODE_PRIVATE);
        SharedPreferences.Editor editor = userData.edit();

        if (user != null) {
            editor.putString("id", user.getId());
            editor.putString("name", user.getName());
            editor.putString("email", user.getEmail());
            editor.putString("imgUrl", user.getImgUrl());
            editor.putString("phoneNo", user.getPhoneNo());
            editor.putString("cnicNo", user.getCnicNo());
            editor.apply();
        }

    }

}
