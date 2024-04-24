package com.example.alertify_user.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.alertify_user.R;
import com.example.alertify_user.databinding.FragmentEmergencyServiceBinding;
import com.example.alertify_user.main_utils.LatLngWrapper;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

    private SharedPreferences userData;

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
        userData = getContext().getSharedPreferences("userData", MODE_PRIVATE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.emergencyBtn) {
            new MaterialAlertDialogBuilder(requireContext()).setMessage("Are you sure you want to send emergency request?").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getLastLocation();
                }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).show();
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
                    setEmergencyRequestToModel(policeStation.getDepAdminId());
                    break;
                }
            }

            if (appropriatePoliceStationName == null) {
                Toast.makeText(getActivity(), "No police station found regarding your location", Toast.LENGTH_SHORT).show();
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
            dataObj.put("title", userData.getString("name",""));
            dataObj.put("body", "needs help right now.");

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

    private void setEmergencyRequestToModel(String depAdminId) {
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

            addEmergencyRequestToDB(emergencyServiceModel, depAdminId);
        }
    }

    private void addEmergencyRequestToDB(EmergencyServiceModel emergencyServiceRequest, String depAdminId) {
        emergencyRequestsRef
                .child(emergencyServiceRequest.getRequestId())
                .setValue(emergencyServiceRequest)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            updateDepAdminEmergencyRequestList(depAdminId, emergencyServiceRequest.getRequestId());
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateDepAdminEmergencyRequestList(String depAdminId, String requestId) {
        depAdminRef.child(depAdminId).child("emergencyRequestList").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> requestList = new ArrayList<>();
                for (DataSnapshot snapshotData : dataSnapshot.getChildren()) {
                    requestList.add(snapshotData.getValue(String.class));
                }
                // Check if the new policeStationId already exists in the list
                if (!requestList.contains(requestId)) {
                    // If it doesn't exist, add it to the list
                    requestList.add(requestId);

                    // Update the value of policeStationList in the database
                    depAdminRef.child(depAdminId).child("emergencyRequestList").setValue(requestList).addOnSuccessListener(aVoid -> {

                        getDepAdminFCMToken(depAdminId);
                        Toast.makeText(getActivity(), "Emergency Service request sent", Toast.LENGTH_SHORT).show();

                    }).addOnFailureListener(e -> {
                        // Handle failure
                        Toast.makeText(requireActivity(), "Failed to add emergencyRequestList to DepAdminRef: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Toast.makeText(getActivity(), "Emergency Service request sent", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle onCancelled event
                Toast.makeText(requireActivity(), "Failed to retrieve depAdmin emergency Request list: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getDepAdminFCMToken(String depAdminId) {
        depAdminRef.child(depAdminId).child("depAdminFCMToken").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                sendNotification(task.getResult().getValue().toString());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(requireActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getCurrentDateTime() {
        // Create a SimpleDateFormat to format the date and time as desired
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy hh:mm a");
        return dateFormat.format(new Date());
    }
}