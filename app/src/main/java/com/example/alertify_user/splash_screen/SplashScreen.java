package com.example.alertify_user.splash_screen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alertify_user.main_utils.MainActivity;
import com.example.alertify_user.R;
import com.example.alertify_user.authentication.LoginSignup;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splach_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
                boolean check = pref.getBoolean("flag", false);
                Intent intent;

                if (check) {
                    intent = new Intent(SplashScreen.this, MainActivity.class);
                } else {
                    intent = new Intent(SplashScreen.this, LoginSignup.class);
                }

                startActivity(intent);
                finish();
            }
        }, 2000);
    }
}