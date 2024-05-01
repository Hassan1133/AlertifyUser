package com.example.alertify_user.activities;

import static com.example.alertify_user.constants.Constants.LAWS_REF;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alertify_user.R;
import com.example.alertify_user.adapters.CrimeLawsAdp;
import com.example.alertify_user.constants.Constants;
import com.example.alertify_user.databinding.ActivityLawsDetailsBinding;
import com.example.alertify_user.models.CrimeLawsModel;
import com.example.alertify_user.models.LawsModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LawsDetailsActivity extends AppCompatActivity {

    private ActivityLawsDetailsBinding binding;

    private List<CrimeLawsModel> crimeLaws;

    private DatabaseReference lawsRef;

    private LawsModel lawsModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLawsDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
    }
    private void init()
    {
        getDataFromIntent();
        lawsRef = FirebaseDatabase.getInstance().getReference(Constants.ALERTIFY_LAWS_REF);
        crimeLaws = new ArrayList<>();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(LawsDetailsActivity.this));
        fetchLaws();
    }

    private void getDataFromIntent() {
        lawsModel = (LawsModel) getIntent().getSerializableExtra("lawsModel");
        assert lawsModel != null;
        binding.crimeType.setText(lawsModel.getCrimeType());
    }

    private void fetchLaws() {
        lawsRef.child(lawsModel.getCrimeType()).child(LAWS_REF).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    crimeLaws.clear();

                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        crimeLaws.add(dataSnapshot.getValue(CrimeLawsModel.class));
                    }

                    setDataToRecycler(crimeLaws);


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LawsDetailsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setDataToRecycler(List<CrimeLawsModel> laws) {
        CrimeLawsAdp adp = new CrimeLawsAdp(LawsDetailsActivity.this, laws);
        binding.recyclerView.setAdapter(adp);
    }
}