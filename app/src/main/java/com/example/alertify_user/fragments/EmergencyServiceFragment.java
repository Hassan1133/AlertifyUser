package com.example.alertify_user.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.alertify_user.R;
import com.example.alertify_user.databinding.FragmentEmergencyServiceBinding;
import com.example.alertify_user.main_utils.LatLngWrapper;
import com.example.alertify_user.main_utils.LoadingDialog;
import com.example.alertify_user.models.DepAdminModel;
import com.example.alertify_user.models.EmergencyServiceModel;
import com.example.alertify_user.models.PoliceStationModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.PolyUtil;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EmergencyServiceFragment extends Fragment implements View.OnClickListener {

    private FragmentEmergencyServiceBinding binding;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private double userCurrentLatitude;
    private double userCurrentLongitude;
    private DatabaseReference policeStationsRef, depAdminRef, emergencyRequestsRef;
    private ArrayList<PoliceStationModel> policeStations;
    private ArrayList<DepAdminModel> depAdmins;

    private String appropriatePoliceStationName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEmergencyServiceBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }

    private void init() {
        binding.emergencyBtn.setOnClickListener(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        policeStationsRef = FirebaseDatabase.getInstance().getReference("AlertifyPoliceStations");
        policeStations = new ArrayList<>();
        depAdmins = new ArrayList<>();
        depAdminRef = FirebaseDatabase.getInstance().getReference("AlertifyDepAdmin");
        emergencyRequestsRef = FirebaseDatabase.getInstance().getReference("AlertifyEmergencyRequests");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.emergencyBtn:
                getLastLocation();
                break;
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

                if (location != null) {
                    userCurrentLatitude = location.getLatitude();
                    userCurrentLongitude = location.getLongitude();
                    getPoliceStationsData();
                } else {
                    Toast.makeText(getActivity(), "Unable to retrieve location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getPoliceStationsData() {
        policeStationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                policeStations.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        PoliceStationModel policeStationModel = dataSnapshot.getValue(PoliceStationModel.class);
                        policeStations.add(policeStationModel);
                    }
                    getAppropriatePoliceStation();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAppropriatePoliceStation() {// Find the appropriate police station
        if (policeStations != null && !policeStations.isEmpty()) { // Check if the list is not null and not empty


            for (PoliceStationModel policeStation : policeStations) {

                List<LatLng> latLngPoints = convertLatLngWrapperList(policeStation.getBoundaries());

                if (isLocationInsidePoliceStationArea(new LatLng(userCurrentLatitude, userCurrentLongitude), latLngPoints)) {
                    appropriatePoliceStationName = policeStation.getPoliceStationName();
                    getDepartmentAdminData(appropriatePoliceStationName);
                    break;
                }
            }

            if (appropriatePoliceStationName == null) {
                Toast.makeText(getActivity(), "No police station found regarding your location", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void getDepartmentAdminData(String policeStationName) {
        depAdminRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                depAdmins.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        DepAdminModel depAdminModel = dataSnapshot.getValue(DepAdminModel.class);
                        depAdmins.add(depAdminModel);
                    }
                    getAppropriateDepAdminData(policeStationName, depAdmins);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAppropriateDepAdminData(String policeStation, ArrayList<DepAdminModel> depAdmins) {

        for (DepAdminModel depAdmin : this.depAdmins) {
            if (depAdmin.getDepAdminPoliceStation().matches(policeStation)) {
                sendNotification(depAdmin.getDepAdminFCMToken());
                setEmergencyRequestToModel();
                return;
            }
        }
    }

    private boolean isLocationInsidePoliceStationArea(LatLng location, List<LatLng> latLngPoints) {
        if (latLngPoints != null) {
            return PolyUtil.containsLocation(location, latLngPoints, true);
        }
        return false;
    }

    private List<LatLng> convertLatLngWrapperList(List<LatLngWrapper> latLngWrapperList) {
        List<LatLng> latLngList = new ArrayList<>();

        for (LatLngWrapper latLngWrapper : latLngWrapperList) {
            LatLng latLng = new LatLng(latLngWrapper.getLatitude(), latLngWrapper.getLongitude());
            latLngList.add(latLng);
        }

        return latLngList;
    }

    void sendNotification(String token) {

        try {
            JSONObject jsonObject = new JSONObject();

            JSONObject dataObj = new JSONObject();
            dataObj.put("title", "hassan");
            dataObj.put("body", "message");

            jsonObject.put("data", dataObj);
            jsonObject.put("to", token);

            callApi(jsonObject);

        } catch (Exception e) {

        }
    }

    void callApi(JSONObject jsonObject) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer AAAA-aYabrI:APA91bHATVQVDYwB1qVX2_O8D1wWhVy0weiIPNJ5-G76w7WMSqcyqVs3HkOqJw8qYXlEl5YvG_62HgIyURoeNPpJN5n3v3jVeNtsGTKmle7tw7tuxxhrtpyCd0zcniEjIgb9aldbIG0l")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            }
        });
    }

    private void setEmergencyRequestToModel() {
        if (appropriatePoliceStationName != null && userCurrentLatitude != 0 && userCurrentLongitude != 0) {

            SharedPreferences userData = getActivity().getSharedPreferences("userData", MODE_PRIVATE);

            EmergencyServiceModel emergencyServiceModel = new EmergencyServiceModel();
            emergencyServiceModel.setRequestId(emergencyRequestsRef.push().getKey());
            emergencyServiceModel.setRequestStatus("unseen");
            emergencyServiceModel.setRequestDateTime(getCurrentDateTime());
            emergencyServiceModel.setUserId(userData.getString("id", ""));
            emergencyServiceModel.setUserName(userData.getString("name", ""));
            emergencyServiceModel.setUserEmail(userData.getString("email", ""));
            emergencyServiceModel.setUserCnic(userData.getString("cnicNo", ""));
            emergencyServiceModel.setUserPhoneNo(userData.getString("phoneNo", ""));
            emergencyServiceModel.setPoliceStation(appropriatePoliceStationName);
            emergencyServiceModel.setUserCurrentLatitude(userCurrentLatitude);
            emergencyServiceModel.setUserCurrentLongitude(userCurrentLongitude);

            addEmergencyRequestToDB(emergencyServiceModel);
        }
    }

    private void addEmergencyRequestToDB(EmergencyServiceModel emergencyServiceRequest) {
        emergencyRequestsRef
                .child(emergencyServiceRequest.getRequestId())
                .setValue(emergencyServiceRequest)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Service request sent", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getCurrentDateTime() {
        // Create a SimpleDateFormat to format the date and time as desired
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy hh:mm a");
        String formattedDate = dateFormat.format(new Date());
        return formattedDate;
    }
}