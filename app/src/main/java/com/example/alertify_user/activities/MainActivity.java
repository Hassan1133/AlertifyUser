package com.example.alertify_user.activities;

import static com.example.alertify_user.constants.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.alertify_user.R;
import com.example.alertify_user.fragments.Complaints_Fragment;
import com.example.alertify_user.fragments.EmergencyRequestFragment;
import com.example.alertify_user.fragments.PoliceStationFragment;
import com.example.alertify_user.fragments.Records_Fragment;
import com.example.alertify_user.main_utils.LocationPermissionUtils;
import com.example.alertify_user.main_utils.StoragePermissionUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private View headerView;
    private ImageView toolBarBtn;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private FirebaseAuth firebaseAuth;
    private TextView userName, userEmail;
    private BottomNavigationView bottom_navigation;

    private Intent intent;

    private DatabaseReference userRef;

    private LocationPermissionUtils locationPermissionUtils;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_layout);
        initialize(); // initialization method for initializing variables
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void initialize() {
        toolBarBtn = findViewById(R.id.tool_bar_menu);
        toolBarBtn.setOnClickListener(this);

        drawer = findViewById(R.id.drawer);
        navigationView = findViewById(R.id.navigation);
        headerView = navigationView.getHeaderView(0);
        userName = headerView.findViewById(R.id.user_name);
        userEmail = headerView.findViewById(R.id.user_email);

        firebaseAuth = FirebaseAuth.getInstance();

        bottom_navigation = findViewById(R.id.bottom_navigation);

        userRef = FirebaseDatabase.getInstance().getReference("AlertifyUser");

        setProfileData();

        locationPermissionUtils = new LocationPermissionUtils(this);
        locationPermissionUtils.checkAndRequestPermissions();
        locationPermissionUtils.getLocationPermission();

        StoragePermissionUtils storagePermissionUtils = new StoragePermissionUtils(this);
        storagePermissionUtils.checkStoragePermission();

        navigationSelection(); // selection method for navigation items
        bottomNavigationSelection();
        checkDepAdminBlockOrNot();
        bottom_navigation.setSelectedItemId(R.id.complaints);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tool_bar_menu) {
            startDrawer(); // start drawer method for open or close navigation drawer
        }
    }

    private void setProfileData() {
        SharedPreferences userData = getSharedPreferences("userData", MODE_PRIVATE);
        userName.setText(userData.getString("name", ""));

        userEmail.setText(userData.getString("email", ""));

    }

    private void startDrawer() {
        if (!drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START);
        } else {
            drawer.closeDrawer(GravityCompat.START);
        }
    }

    private void navigationSelection() {
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {

                    case R.id.logout:
                        logout();
                        break;
                    case R.id.profile:
                        intent = new Intent(MainActivity.this, EditUserProfileActivity.class);
                        startActivity(intent);
                        drawer.closeDrawer(GravityCompat.START);
                        break;
                    case R.id.home:
                        loadFragment(new Complaints_Fragment());
                        drawer.closeDrawer(GravityCompat.START);
                        break;

                }
                return false;
            }
        });
    }

    private void logout() {

        FirebaseAuth.getInstance().signOut();

        SharedPreferences userData = getSharedPreferences("userData", Context.MODE_PRIVATE);
        SharedPreferences.Editor profileDataEditor = userData.edit();
        profileDataEditor.clear();
        profileDataEditor.apply();

        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.apply();

        intent = new Intent(MainActivity.this, LoginSignupActivity.class);
        startActivity(intent);
        finish();
    }

    private void bottomNavigationSelection() {

        bottom_navigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.complaints:
                        loadFragment(new Complaints_Fragment());
                        return true;
                    case R.id.police_station:
                        loadFragment(new PoliceStationFragment());
                        return true;
                    case R.id.records:
                        loadFragment(new Records_Fragment());
                        return true;
                    case R.id.emergency:
                        loadFragment(new EmergencyRequestFragment());
                        return true;
                }
                return false;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, fragment).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setProfileData();
    }

    private void checkDepAdminBlockOrNot() {

        userRef.child(firebaseAuth.getUid()).child("userStatus").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue(String.class).equals("block")) {
                    SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean("flag", false);
                    editor.apply();

                    Intent intent = new Intent(MainActivity.this, LoginSignupActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if (!locationPermissionUtils.locationPermission) {
                    locationPermissionUtils.getLocationPermission();
                }
            }
        }
    }

}
