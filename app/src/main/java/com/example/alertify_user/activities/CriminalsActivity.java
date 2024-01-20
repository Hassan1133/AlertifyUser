package com.example.alertify_user.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alertify_user.R;
import com.example.alertify_user.databinding.ActivityCriminalsBinding;

public class CriminalsActivity extends AppCompatActivity {

    private ActivityCriminalsBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCriminalsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}