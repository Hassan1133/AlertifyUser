package com.example.alertify_user.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alertify_user.R;
import com.example.alertify_user.databinding.ActivityLawsBinding;

public class LawsActivity extends AppCompatActivity {
    private ActivityLawsBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLawsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}