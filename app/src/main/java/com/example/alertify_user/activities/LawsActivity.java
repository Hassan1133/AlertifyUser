package com.example.alertify_user.activities;

import static com.example.alertify_user.constants.Constants.ALERTIFY_LAWS_REF;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alertify_user.R;
import com.example.alertify_user.adapters.LawsAdp;
import com.example.alertify_user.databinding.ActivityLawsBinding;
import com.example.alertify_user.models.LawsModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LawsActivity extends AppCompatActivity {
    private ActivityLawsBinding binding;

    private DatabaseReference lawsRef;

    private List<LawsModel> lawCrimes;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLawsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
    }
    private void init()
    {
        lawsRef = FirebaseDatabase.getInstance().getReference(ALERTIFY_LAWS_REF);

        binding.crimesRecycler.setLayoutManager(new LinearLayoutManager(LawsActivity.this));

        lawCrimes = new ArrayList<LawsModel>();

        fetchData();
        binding.lawsSearchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                search(newText);
                return true;
            }
        });
    }

    private void search(String newText) {
        ArrayList<LawsModel> searchList = new ArrayList<>();
        for (LawsModel i : lawCrimes) {
            if (i.getCrimeType().toLowerCase().contains(newText.toLowerCase())) {
                searchList.add(i);
            }
        }
        setDataToRecycler(searchList);
    }
    private void fetchData() {

        binding.lawsProgressbar.setVisibility(View.VISIBLE);
        lawsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                lawCrimes.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    lawCrimes.add(dataSnapshot.getValue(LawsModel.class));
                }

                binding.lawsProgressbar.setVisibility(View.GONE);

                setDataToRecycler(lawCrimes);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LawsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setDataToRecycler(List<LawsModel> lawCrimes) {
        LawsAdp adp = new LawsAdp(LawsActivity.this, lawCrimes);
        binding.crimesRecycler.setAdapter(adp);
    }
}