package com.example.alertify_user.authentication;

import static android.app.Activity.RESULT_OK;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.alertify_user.R;
import com.example.alertify_user.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class SignupFragment extends Fragment implements View.OnClickListener {

    private ImageView pick_img_icon, userImg;

    private TextInputEditText name, phone, cnic, email, password;

    private Button signupBtn;

    private Uri imageUri;

    private UserModel user;

    private FirebaseAuth firebaseAuth;

    private StorageReference firebaseStorageReference;

    private DatabaseReference firebaseDatabaseReference;

    private Dialog loadingDialog;

    private ProgressBar loadingProgressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.signup, container, false);
        init(view);
        return view;

    }

    private void init(@NonNull View view) // method for widgets or variables initialization
    {
        pick_img_icon = view.findViewById(R.id.pick_img_icon);
        pick_img_icon.setOnClickListener(this);

        name = view.findViewById(R.id.name);
        phone = view.findViewById(R.id.phone);
        cnic = view.findViewById(R.id.cnic);
        email = view.findViewById(R.id.email);
        password = view.findViewById(R.id.password);

        signupBtn = view.findViewById(R.id.signup_btn);
        signupBtn.setOnClickListener(this);


        userImg = view.findViewById(R.id.image);

        firebaseAuth = FirebaseAuth.getInstance();

        firebaseStorageReference = FirebaseStorage.getInstance().getReference();

        firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference("AlertifyUser");
    }

    @Override
    public void onClick(@NonNull View v) {
        switch (v.getId()) {
            case R.id.pick_img_icon:
                chooseImage();
                break;

            case R.id.signup_btn:
                checkUserCnicPhoneExists(cnic.getText().toString().trim(), phone.getText().toString().trim());
                break;
        }
    }

    private void createLoadingDialog()
    {
        loadingDialog = new Dialog(getActivity());
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.show();
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView loadingTxt = loadingDialog.findViewById(R.id.loading);
        loadingTxt.setText("Signing up....");

        loadingProgressBar = loadingDialog.findViewById(R.id.profile_progressbar);

        loadingProgressBar.setVisibility(View.VISIBLE);
    }
    private void checkUserCnicPhoneExists(String cnicNo, String phoneNo) {


        firebaseDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            int count = 0;
            boolean check = false;

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    // data exists in database
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {

                        UserModel user = userSnapshot.getValue(UserModel.class);

                        count++;

                        if (user.getPhoneNo().equals(phoneNo) || user.getCnicNo().equals(cnicNo)) {
                            if(user.getPhoneNo().equals(phoneNo))
                            {
                                phone.setText("");
                                Toast.makeText(getActivity(), "Phone number already exists. Please choose a different one", Toast.LENGTH_SHORT).show();
                                phone.setError("Phone number already exists. Please choose a different one");
                                check = true;
                            }
                            if (user.getCnicNo().equals(cnicNo)) {
                                cnic.setText("");
                                Toast.makeText(getActivity(), "CNIC number already exists. Please choose a different one", Toast.LENGTH_SHORT).show();
                                cnic.setError("CNIC number already exists. Please choose a different one");
                                check = true;
                            }
                            return;
                        } else if (count == snapshot.getChildrenCount()) {
                            if (!check) {
                                createAccount();
                            }
                        }
                    }
                }
                else {
                    createAccount();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void createAccount() // method for create account
    {

        if (isValid()) {

            createLoadingDialog();

            user = new UserModel();
            user.setName(name.getText().toString().trim());
            user.setPhoneNo(phone.getText().toString().trim());
            user.setCnicNo(cnic.getText().toString().trim());
            user.setEmail(email.getText().toString().trim());
            user.setType("user");
            user.setUserStatus("unblock");

            firebaseAuth
                    .createUserWithEmailAndPassword(user.getEmail(), password.getText().toString().trim())
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                user.setId(firebaseAuth.getUid());
                                uploadImage(user);
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            loadingDialog.dismiss();
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }

    private void uploadImage(UserModel user) // method for upload image
    {

        StorageReference strRef = firebaseStorageReference.child("AlertifyUserImages/" + firebaseAuth.getUid());

        strRef
                .putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        strRef
                                .getDownloadUrl()
                                .addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {

                                        user.setImgUrl(task.getResult().toString());
                                        addToDB(user);

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        loadingDialog.dismiss();
                                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        loadingDialog.dismiss();
                    }
                });

    }

    private void addToDB(@NonNull UserModel user) // method for add data to the database
    {
        firebaseDatabaseReference
                .child(user.getId())
                .setValue(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            loadingDialog.dismiss();
                            Toast.makeText(getContext(), "Signed up Successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getActivity(), LoginSignup.class);
                            startActivity(intent);
                            getActivity().finish();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingDialog.dismiss();
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isValid() // method for data validation
    {
        boolean valid = true;

        if (imageUri == null) {
            Toast.makeText(getContext(), "select your image", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (name.getText().length() < 3) {
            name.setError("enter valid name");
            valid = false;
        }
        if (phone.getText().length() < 11) {
            phone.setError("enter valid phone no");
            valid = false;
        }
        if (cnic.getText().length() < 13 || cnic.getText().length() > 13) {
            cnic.setError("enter valid cnic no");
            valid = false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.getText()).matches()) {
            email.setError("enter valid email");
            valid = false;
        }
        if (password.getText().length() < 6) {
            password.setError("enter valid password");
            valid = false;
        }

        return valid;
    }

    private void chooseImage() // method for get image from gallery
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            userImg.setImageURI(imageUri);
        }
    }
}
