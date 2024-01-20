package com.example.alertify_user.activities;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alertify_user.R;
import com.example.alertify_user.databinding.ActivityCrimesBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CrimesActivity extends AppCompatActivity {

    private ActivityCrimesBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCrimesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
    }
    private void init()
    {

    }
}