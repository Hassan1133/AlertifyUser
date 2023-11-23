package com.example.alertify_user.main_utils;

import static com.example.alertify_user.constants.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.example.alertify_user.constants.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;
import static com.example.alertify_user.constants.Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.alertify_user.R;
import com.example.alertify_user.authentication.LoginSignup;
import com.example.alertify_user.complaints.Complaints_Fragment;
import com.example.alertify_user.police_station.PoliceStationFragment;
import com.example.alertify_user.records.Records_Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.DexterBuilder;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private View headerView;
    private ImageView toolBarBtn;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private FirebaseAuth firebaseAuth;
    private CircleImageView userImage;
    private TextView userName, userEmail;
    private BottomNavigationView bottom_navigation;

    private Intent intent;

    private DatabaseReference userRef;

    private LocationPermissionUtils locationPermissionUtils;

    private StoragePermissionUtils storagePermissionUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_layout);

        initialize(); // initialization method for initializing variables
        navigationSelection(); // selection method for navigation items
        bottomNavigationSelection();
        checkDepAdminBlockOrNot();
        loadFragment(new Complaints_Fragment());

    }

    private void initialize() {
        toolBarBtn = findViewById(R.id.tool_bar_menu);
        toolBarBtn.setOnClickListener(this);

        drawer = findViewById(R.id.drawer);
        navigationView = findViewById(R.id.navigation);
        headerView = navigationView.getHeaderView(0);
        userImage = headerView.findViewById(R.id.circle_img);
        userName = headerView.findViewById(R.id.user_name);
        userEmail = headerView.findViewById(R.id.user_email);

        firebaseAuth = FirebaseAuth.getInstance();

        bottom_navigation = findViewById(R.id.bottom_navigation);

        userRef = FirebaseDatabase.getInstance().getReference("AlertifyUser");

        setProfileData();

        locationPermissionUtils = new LocationPermissionUtils(this);
        locationPermissionUtils.checkAndRequestPermissions();
        locationPermissionUtils.getLocationPermission();

        storagePermissionUtils = new StoragePermissionUtils(this);
        storagePermissionUtils.checkStoragePermission();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tool_bar_menu:
                startDrawer(); // start drawer method for open or close navigation drawer
                break;
        }
    }

    private void setProfileData() {
        SharedPreferences userData = getSharedPreferences("userData", MODE_PRIVATE);
        userName.setText(userData.getString("name", ""));

        userEmail.setText(userData.getString("email", ""));

        Glide.with(getApplicationContext()).load(userData.getString("imgUrl", "")).into(userImage);
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
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {

                    case R.id.logout:

                        SharedPreferences pref = getSharedPreferences("login", MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putBoolean("flag", false);
                        editor.apply();

                        intent = new Intent(MainActivity.this, LoginSignup.class);
                        startActivity(intent);
                        finish();
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

    private void bottomNavigationSelection() {

        bottom_navigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
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

                    Intent intent = new Intent(MainActivity.this, LoginSignup.class);
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
