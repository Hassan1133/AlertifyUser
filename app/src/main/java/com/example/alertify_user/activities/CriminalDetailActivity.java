package com.example.alertify_user.activities;


import static com.example.alertify_user.constants.Constants.ALERTIFY_CRIMINALS_REF;
import static com.example.alertify_user.constants.Constants.FIR_REF;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alertify_user.adapters.CriminalCrimesAdp;
import com.example.alertify_user.databinding.ActivityCriminalDetailBinding;
import com.example.alertify_user.models.CriminalCrimesModel;
import com.example.alertify_user.models.CriminalsModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CriminalDetailActivity extends AppCompatActivity {

    private ActivityCriminalDetailBinding binding;

    private CriminalsModel criminalsModel;

    private DatabaseReference criminalsRef;

    private List<CriminalCrimesModel> criminalCrimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCriminalDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
    }

    private void init() {
        getDataFromIntent();
        criminalsRef = FirebaseDatabase.getInstance().getReference(ALERTIFY_CRIMINALS_REF);
        criminalCrimes = new ArrayList<>();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(CriminalDetailActivity.this));
        fetchFIR();
    }

    private void fetchFIR() {
        criminalsRef.child(criminalsModel.getCriminalCnic()).child(FIR_REF).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    criminalCrimes.clear();

                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        criminalCrimes.add(dataSnapshot.getValue(CriminalCrimesModel.class));
                    }

                    setDataToRecycler(criminalCrimes);


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CriminalDetailActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setDataToRecycler(List<CriminalCrimesModel> criminalCrimes) {
        CriminalCrimesAdp adp = new CriminalCrimesAdp(CriminalDetailActivity.this, criminalCrimes);
        binding.recyclerView.setAdapter(adp);
    }

    private void getDataFromIntent() {
        criminalsModel = (CriminalsModel) getIntent().getSerializableExtra("criminalModel");
        assert criminalsModel != null;
        binding.criminalName.setText(criminalsModel.getCriminalName());
        binding.criminalCnic.setText(criminalsModel.getCriminalCnic());

    }
}