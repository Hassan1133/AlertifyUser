package com.example.alertify_user.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.alertify_user.R;
import com.example.alertify_user.databinding.ActivityMapsBinding;
import com.example.alertify_user.interfaces.RecognitionCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap googleMap;
    private ActivityMapsBinding binding;
    private Geocoder geocoder;

    private String selectedAddress;

    private SupportMapFragment mapFragment;

    private RecognitionCallback recognitionCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
    }

    private void init()
    {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        searchLocation();

        binding.searchVoiceBtn.setOnClickListener(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

//        get location by clicking on marker
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {

                getAddress(marker.getPosition().latitude, marker.getPosition().longitude);
                return false;
            }
        });
        //        get location by clicking on any where on map
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                getAddress(latLng.latitude, latLng.longitude);
            }
        });

        //        get location current location and customization of current location button
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        googleMap.setMyLocationEnabled(true);
        View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.setMargins(0, 0, 0, 20);
    }

    private void searchLocation() {
        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                googleMap.clear();

                if (checkConnection()) {
                    String location = binding.searchView.getQuery().toString();
                    List<Address> addressList = null;

                    if (!location.isEmpty()) {
                        geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());

                        try {
                            addressList = geocoder.getFromLocationName(location, 1);
                        } catch (IOException e) {
                            Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        if (!addressList.isEmpty()) {
                            Address address = addressList.get(0);
                            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                            googleMap.addMarker(new MarkerOptions().position(latLng).title(location));
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
                        } else {
                            Toast.makeText(MapsActivity.this, "Please select a valid location", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(MapsActivity.this, "Check your internet connection", Toast.LENGTH_SHORT).show();

                }


                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private boolean checkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void getAddress(double latitude, double longitude) {
        geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
        List<Address> addresses = null;

        if (latitude != 0 && longitude != 0) {
            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1);
            } catch (IOException e) {
                Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            if (addresses != null) {

                if (!addresses.isEmpty()) {
                    selectedAddress = addresses.get(0).getAddressLine(0);

                    if (!selectedAddress.isEmpty()) {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("latitude", latitude);
                        returnIntent.putExtra("longitude", longitude);
                        returnIntent.putExtra("address", selectedAddress);
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
                    } else {
                        Toast.makeText(this, "Please choose a valid location", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(this, "Please choose a valid location", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please choose a valid location", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please choose a valid location", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId())
        {
            case R.id.searchVoiceBtn:
                startVoiceRecognition(new RecognitionCallback() {
                    @Override
                    public void onRecognitionComplete(String result) {
                        if (!result.isEmpty()) {
                            binding.searchView.setQuery(result, true);
                        } else {
                            Toast.makeText(MapsActivity.this, "Voice recognition failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onRecognitionFailure(String errorMessage) {
                        Toast.makeText(MapsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    private void startVoiceRecognition(RecognitionCallback callback) {

        this.recognitionCallback = callback;

        // Start Speech Recognition Intent
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()); // Specify the language, for example, en-US for English
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something...");

        voiceRecognitionResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> voiceRecognitionResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                ArrayList<String> recognizedTimeResult = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                recognitionCallback.onRecognitionComplete(recognizedTimeResult.get(0));
                recognizedTimeResult.clear();
            }
        }
    });
}