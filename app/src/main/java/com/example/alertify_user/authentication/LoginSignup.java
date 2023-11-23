package com.example.alertify_user.authentication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.alertify_user.R;
import com.google.android.material.tabs.TabLayout;

import com.example.alertify_user.adapter.ViewPagerAdapter;

public class LoginSignup extends AppCompatActivity {

    TabLayout tabLayout;
    ViewPagerAdapter viewPagerAdapter;
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_signup);

        init();

    }

    private void init() // initialization of widgets
    {

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        // set fragments and titles to the adapter
        viewPagerAdapter.addFragment(new LoginFragment(), "LOGIN");
        viewPagerAdapter.addFragment(new SignupFragment(), "SIGNUP");

        viewPager = findViewById(R.id.view_pager);
        // set adapter on viewpager
        viewPager.setAdapter(viewPagerAdapter);

        tabLayout = findViewById(R.id.tabs);
        // set tabLayout with viewpager
        tabLayout.setupWithViewPager(viewPager);
    }

}