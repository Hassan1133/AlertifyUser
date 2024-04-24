package com.example.alertify_user.fragments;

import static android.content.Context.MODE_PRIVATE;
import static com.example.alertify_user.constants.Constants.ALERTIFY_CRIMES_REF;
import static com.example.alertify_user.constants.Constants.ALERTIFY_DEP_ADMIN_REF;
import static com.example.alertify_user.constants.Constants.ALERTIFY_POLICE_STATIONS_REF;
import static com.example.alertify_user.constants.Constants.EVIDENCE_FILE_SIZE_LIMIT;
import static com.example.alertify_user.constants.Constants.USERS_COMPLAINTS_REF;
import static com.example.alertify_user.constants.Constants.USERS_REF;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.alertify_user.R;
import com.example.alertify_user.activities.MapsActivity;
import com.example.alertify_user.adapters.ComplaintsAdapter;
import com.example.alertify_user.adapters.DropDownAdapter;
import com.example.alertify_user.databinding.ComplaintDialogBinding;
import com.example.alertify_user.databinding.ComplaintsFragmentBinding;
import com.example.alertify_user.interfaces.RecognitionCallback;
import com.example.alertify_user.interfaces.TranslationCallback;
import com.example.alertify_user.main_utils.LanguageTranslator;
import com.example.alertify_user.main_utils.LatLngWrapper;
import com.example.alertify_user.main_utils.LoadingDialog;
import com.example.alertify_user.main_utils.LocationPermissionUtils;
import com.example.alertify_user.main_utils.NetworkUtils;
import com.example.alertify_user.models.ComplaintModel;
import com.example.alertify_user.models.CrimesModel;
import com.example.alertify_user.models.PoliceStationModel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.maps.android.PolyUtil;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Complaints_Fragment extends Fragment implements View.OnClickListener {

    private ComplaintsFragmentBinding binding;
    private ComplaintDialogBinding complaintDialogBinding;
    private Dialog complaintDialog;
    private String appropriatePoliceStationName;
    private ArrayList<String> crimesList;
    private double selectedCrimeLatitude, selectedCrimeLongitude;
    private ArrayList<PoliceStationModel> policeStations;
    private ComplaintModel complaintModel;
    private LocationPermissionUtils permissionUtils;
    private StorageReference firebaseStorageReference;
    private DatabaseReference complaintsRef, policeStationsRef, crimesRef, userRef, depAdminRef;
    private List<ComplaintModel> complaints;
    private ComplaintsAdapter complaintsAdapter;
    private Uri evidenceUri;
    private RecognitionCallback recognitionCallback;
    private Dialog loadingDialog;

    private SharedPreferences userData;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = ComplaintsFragmentBinding.inflate(inflater, container, false);
        init();
        Log.d("TAGCheck", "onCreateView: run");
        return binding.getRoot();
    }

    private void init() {
        binding.addComplaintBtn.setOnClickListener(this);

        crimesRef = FirebaseDatabase.getInstance().getReference(ALERTIFY_CRIMES_REF);
        crimesList = new ArrayList<>();

        policeStationsRef = FirebaseDatabase.getInstance().getReference(ALERTIFY_POLICE_STATIONS_REF);
        policeStations = new ArrayList<>();

        permissionUtils = new LocationPermissionUtils(getActivity());

        complaintsRef = FirebaseDatabase.getInstance().getReference(USERS_COMPLAINTS_REF); // firebase initialization

        firebaseStorageReference = FirebaseStorage.getInstance().getReference();

        complaints = new ArrayList<ComplaintModel>();

        binding.complaintsRecycler.setLayoutManager(new GridLayoutManager(getActivity(), 2));

        userRef = FirebaseDatabase.getInstance().getReference(USERS_REF);
        depAdminRef = FirebaseDatabase.getInstance().getReference(ALERTIFY_DEP_ADMIN_REF);

        userData = getContext().getSharedPreferences("userData", MODE_PRIVATE);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.addComplaintBtn) {
            if (NetworkUtils.isInternetAvailable(requireActivity())) {
                // Internet is available, call the method for creating dialog
                createComplaintDialog();

            } else {
                // Internet is not available, show a message to the user
                Toast.makeText(getActivity(), "Please turn on your internet", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!isAdded()) {
            return;
        }

        fetchComplaintsData();
        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                search(newText);
                return true;
            }
        });
    }

    private void search(String newText) {
        ArrayList<ComplaintModel> searchList = new ArrayList<>();
        for (ComplaintModel i : complaints) {
            if (i.getCrimeType().toLowerCase().contains(newText.toLowerCase()) || i.getComplaintDateTime().toLowerCase().contains(newText.toLowerCase()) || i.getCrimeDate().toLowerCase().contains(newText.toLowerCase()) || i.getCrimeTime().toLowerCase().contains(newText.toLowerCase()) || i.getPoliceStation().toLowerCase().contains(newText.toLowerCase()) || i.getCrimeLocation().toLowerCase().contains(newText.toLowerCase()) || i.getInvestigationStatus().toLowerCase().contains(newText.toLowerCase())) {
                searchList.add(i);
            }
        }
        setDataToRecycler(searchList);
    }
    private void createComplaintDialog() {
        complaintDialogBinding = ComplaintDialogBinding.inflate(LayoutInflater.from(getActivity()));
        complaintDialog = new Dialog(requireActivity());
        complaintDialog.setContentView(complaintDialogBinding.getRoot());
        complaintDialog.setCancelable(false);
        complaintDialog.show();
        complaintDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        fetchCrimeTypeForDropDown();

        complaintDialogBinding.crimeLocationLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MapsActivity.class);

                if (permissionUtils.isMapsEnabled()) {
                    if (permissionUtils.isLocationPermissionGranted()) {
                        mapsActivityResultLauncher.launch(intent);
                    } else {
                        permissionUtils.getLocationPermission();
                    }
                }
            }

        });

        complaintDialogBinding.crimeDateLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDateFromDatePicker();
            }
        });
        complaintDialogBinding.crimeTimeLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTimeFromTimePicker();
            }
        });

        complaintDialogBinding.crimeTimeVoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVoiceRecognition(new RecognitionCallback() {
                    @Override
                    public void onRecognitionComplete(String result) {
                        if (!result.isEmpty()) {
                            String formattedTime = formatSpeechTime(result);
                            if (formattedTime != null) {
                                // Set the formatted time to your field
                                complaintDialogBinding.crimeTime.setText(formattedTime);
                            } else {
                                // Handle invalid time format
                                Toast.makeText(getActivity(), "Invalid time format", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getActivity(), "Voice recognition failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onRecognitionFailure(String errorMessage) {
                        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        complaintDialogBinding.crimeDateVoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVoiceRecognition(new RecognitionCallback() {
                    @Override
                    public void onRecognitionComplete(String result) {
                        if (!result.isEmpty()) {
                            String correctedDate = correctMonthSpelling(result);
                            if (correctedDate != null) {
                                // Set the formatted time to your field

                                complaintDialogBinding.crimeDate.setText(correctedDate);
                            } else {
                                // Handle invalid time format
                                Toast.makeText(getActivity(), "Invalid date format", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getActivity(), "Voice recognition failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onRecognitionFailure(String errorMessage) {
                        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        complaintDialogBinding.crimeDetailsLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceRecognitionTranslation(new RecognitionCallback() {
                    @Override
                    public void onRecognitionComplete(String result) {
                        if (!result.isEmpty()) {

                            complaintDialogBinding.crimeDetails.setText(result);

                        } else {
                            Toast.makeText(getActivity(), "Voice recognition failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onRecognitionFailure(String errorMessage) {
                        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        complaintDialogBinding.crimeEvidenceLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickEvidence();
            }

        });
        complaintDialogBinding.closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                complaintDialog.dismiss();
            }
        });
        complaintDialogBinding.reportCrimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDataValid()) {
                    loadingDialog = LoadingDialog.showLoadingDialog(getActivity());
                    getPoliceStationsData();
                }
            }
        });
    }

    // Helper method for month spelling correction
    private String correctMonthSpelling(String inputDate) {
        // Define a regular expression for the expected date format
        String regex = "(\\d{1,2})\\s(January|February|March|April|May|Mai|June|July|August|September|October|November|December)\\s(\\d{4})";

        // Check if the inputDate matches the expected format
        if (inputDate.matches(regex)) {
            // Replace "mai" with "May" only
            // (?i): This is a flag indicating a case-insensitive match. It means that the regular expression engine will match "mai" regardless of whether it is uppercase, lowercase, or a mix of both.
            return inputDate.replaceAll("(?i)mai", "May");
        } else {
            return null; // Return null for invalid date format
        }
    }

    private void getDateFromDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
            @Override
            public void onPositiveButtonClick(Long selection) {

                complaintDialogBinding.crimeDate.setText(new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(selection)));
            }
        });

        datePicker.show(getChildFragmentManager(), "tag");
    }

    private String formatSpeechTime(String recognizedText) {
        // Use regular expression to match the desired time format (e.g., 4:45 AM)
        Matcher matcher = Pattern.compile("([1-9]|1[0-2])\\s*:\\s*([0-5]?[0-9])\\s*([aApP][mM])").matcher(recognizedText.replaceAll("\\.", ""));

        if (matcher.find()) {
            // Format the time as needed
            // 04 : 45 AM
            return matcher.group(1) + ":" + matcher.group(2) + " " + matcher.group(3).toUpperCase();
        } else {
            return null; // Return null for invalid time format
        }
    }

    private void getTimeFromTimePicker() {

        // Create a TimePickerDialog with MaterialTimePicker
        MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(Calendar.getInstance().HOUR_OF_DAY)
                .setMinute(Calendar.getInstance().MINUTE)
                .setTitleText(R.string.select_time)
                .build();

        materialTimePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amPm = (materialTimePicker.getHour() >= 12) ? "PM" : "AM";
                // Format the selected time in 12-hour format
                int formattedHour = (materialTimePicker.getHour() == 0 || materialTimePicker.getHour() == 12) ? 12 : materialTimePicker.getHour() % 12;

                // Display the formatted time
                complaintDialogBinding.crimeTime.setText(String.format("%02d:%02d %s", formattedHour, materialTimePicker.getMinute(), amPm));

            }
        });

        materialTimePicker.show(getChildFragmentManager(), "TIME_PICKER_TAG");

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
                LoadingDialog.hideLoadingDialog(loadingDialog);
            }
        });
    }

    private void getAppropriatePoliceStation() {// Find the appropriate police station
        if (policeStations != null && !policeStations.isEmpty()) { // Check if the list is not null and not empty


            for (PoliceStationModel policeStation : policeStations) {

                List<LatLng> latLngPoints = convertLatLngWrapperList(policeStation.getBoundaries());

                if (isLocationInsidePoliceStationArea(new LatLng(selectedCrimeLatitude, selectedCrimeLongitude), latLngPoints)) {
                    appropriatePoliceStationName = policeStation.getPoliceStationName();
                    addDataToModel(policeStation.getDepAdminId());
                    break;
                }
            }

            if (appropriatePoliceStationName == null) {
                LoadingDialog.hideLoadingDialog(loadingDialog);
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

    private void addDataToModel(String depAdminId) {
        complaintModel = new ComplaintModel();
        complaintModel.setCrimeType(complaintDialogBinding.crimeType.getText().toString());
        complaintModel.setCrimeDetails(complaintDialogBinding.crimeDetails.getText().toString().trim());
        complaintModel.setCrimeDate(complaintDialogBinding.crimeDate.getText().toString());
        complaintModel.setCrimeTime(complaintDialogBinding.crimeTime.getText().toString());
        complaintModel.setComplaintDateTime(getCurrentDateTime());
        complaintModel.setCrimeLatitude(selectedCrimeLatitude);
        complaintModel.setCrimeLongitude(selectedCrimeLongitude);
        complaintModel.setCrimeLocation(complaintDialogBinding.crimeLocation.getText().toString());
        complaintModel.setPoliceStation(appropriatePoliceStationName);
        complaintModel.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        complaintModel.setInvestigationStatus("Pending");
        complaintModel.setFeedback("none");
        uploadEvidence(complaintModel, depAdminId);
    }

    private String getCurrentDateTime() {
        // Create a SimpleDateFormat to format the date and time as desired
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy hh:mm a");
        return dateFormat.format(new Date());
    }

    private void uploadEvidence(ComplaintModel complaintModel, String depAdminId) {
        complaintModel.setComplaintId(complaintsRef.push().getKey());

        if (evidenceUri != null) {
            StorageReference strRef = firebaseStorageReference.child("Alertify_Complaints_Evidences/" + complaintModel.getComplaintId());
            strRef.putFile(evidenceUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    strRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            complaintModel.setEvidenceUrl(task.getResult().toString());
                            complaintModel.setEvidenceType(taskSnapshot.getMetadata().getContentType());
                            addToDb(complaintModel, depAdminId);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            LoadingDialog.hideLoadingDialog(loadingDialog);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    LoadingDialog.hideLoadingDialog(loadingDialog);
                }
            });
        } else {
            complaintModel.setEvidenceUrl("");
            complaintModel.setEvidenceType("");
            addToDb(complaintModel, depAdminId);
        }
    }

    private void addToDb(ComplaintModel complaintModel, String depAdminId) {
        complaintsRef.child(complaintModel.getComplaintId()).setValue(complaintModel).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    updateUserComplaintList(complaintModel.getComplaintId(), complaintModel.getUserId(), depAdminId);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                LoadingDialog.hideLoadingDialog(loadingDialog);
            }
        });
    }

    private void updateUserComplaintList(String complaintId, String userId, String depAdminId) {
        userRef.child(userId).child("complaintList").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> complaintList = new ArrayList<>();
                for (DataSnapshot snapshotData : dataSnapshot.getChildren()) {
                    complaintList.add(snapshotData.getValue(String.class));
                }
                // Check if the new policeStationId already exists in the list
                if (!complaintList.contains(complaintId)) {
                    Toast.makeText(getActivity(), "e.getMessage()", Toast.LENGTH_SHORT).show();
                    // If it doesn't exist, add it to the list
                    complaintList.add(complaintId);

                    // Update the value of policeStationList in the database
                    userRef.child(userId).child("complaintList").setValue(complaintList).addOnSuccessListener(aVoid -> {
                        updateDepAdminList(complaintId, depAdminId);
                    }).addOnFailureListener(e -> {
                        // Handle failure
                        Toast.makeText(requireActivity(), "Failed to add Complaint to UserRef: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                } else {
                    LoadingDialog.hideLoadingDialog(loadingDialog);
                    Toast.makeText(getActivity(), "Complaint Reported successfully!", Toast.LENGTH_SHORT).show();
                    complaintDialog.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle onCancelled event
                LoadingDialog.hideLoadingDialog(loadingDialog);
                Toast.makeText(requireActivity(), "Failed to retrieve user complaint list: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDepAdminList(String complaintId, String depAdminId) {
        depAdminRef.child(depAdminId).child("complaintList").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> complaintList = new ArrayList<>();
                for (DataSnapshot snapshotData : dataSnapshot.getChildren()) {
                    complaintList.add(snapshotData.getValue(String.class));
                }
                // Check if the new policeStationId already exists in the list
                if (!complaintList.contains(complaintId)) {
                    // If it doesn't exist, add it to the list
                    complaintList.add(complaintId);

                    // Update the value of policeStationList in the database
                    depAdminRef.child(depAdminId).child("complaintList").setValue(complaintList).addOnSuccessListener(aVoid -> {

                        getDepAdminFCMToken(depAdminId);
                        evidenceUri = null;
                        LoadingDialog.hideLoadingDialog(loadingDialog);
                        Toast.makeText(getActivity(), "Complaint Submitted successfully!", Toast.LENGTH_SHORT).show();
                        complaintDialog.dismiss();

                    }).addOnFailureListener(e -> {
                        // Handle failure
                        Toast.makeText(requireActivity(), "Failed to add Complaint to DepAdminRef: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                } else {
                    LoadingDialog.hideLoadingDialog(loadingDialog);
                    Toast.makeText(getActivity(), "Complaint Submitted successfully!", Toast.LENGTH_SHORT).show();
                    complaintDialog.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle onCancelled event
                LoadingDialog.hideLoadingDialog(loadingDialog);
                Toast.makeText(requireActivity(), "Failed to retrieve user complaint list: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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

    void sendNotification(String token) {

        try {
            JSONObject jsonObject = new JSONObject();

            JSONObject dataObj = new JSONObject();
            dataObj.put("title", userData.getString("name", ""));
            dataObj.put("body", "registered a complaint.");

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
        Request request = new Request.Builder().url(url).post(body).header("Authorization", "Bearer AAAA-aYabrI:APA91bHATVQVDYwB1qVX2_O8D1wWhVy0weiIPNJ5-G76w7WMSqcyqVs3HkOqJw8qYXlEl5YvG_62HgIyURoeNPpJN5n3v3jVeNtsGTKmle7tw7tuxxhrtpyCd0zcniEjIgb9aldbIG0l").build();
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

    private boolean isDataValid() {
        boolean valid = true;

        if (complaintDialogBinding.crimeType.getText().length() == 0) {
            complaintDialogBinding.crimeType.setError("Please select crime type");
            valid = false;
        }
        if (complaintDialogBinding.crimeDetails.getText().length() == 0) {
            complaintDialogBinding.crimeDetails.setError("Please enter crime details");
            valid = false;
        }
        if (complaintDialogBinding.crimeTime.getText().length() == 0) {
            Toast.makeText(getActivity(), "Please select crime time", Toast.LENGTH_SHORT).show();
            valid = false;
        }
        if (complaintDialogBinding.crimeDate.getText().length() == 0) {
            Toast.makeText(getActivity(), "Please select crime date", Toast.LENGTH_SHORT).show();
            valid = false;
        }
        if (complaintDialogBinding.crimeLocation.getText().length() == 0) {
            Toast.makeText(getActivity(), "Please select crime location", Toast.LENGTH_SHORT).show();
            valid = false;
        }
        if (selectedCrimeLatitude == 0 || selectedCrimeLongitude == 0) {
            Toast.makeText(getActivity(), "Please select crime location again", Toast.LENGTH_SHORT).show();
            valid = false;
        }
        return valid;
    }

    private void fetchCrimeTypeForDropDown() {

        crimesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                crimesList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                    CrimesModel crimesModel = dataSnapshot.getValue(CrimesModel.class);

                    crimesList.add(crimesModel.getCrimeType());
                }
                setCrimeTypesToRecycler(crimesList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setCrimeTypesToRecycler(ArrayList<String> list) {
        complaintDialogBinding.crimeType.setAdapter(new DropDownAdapter(getActivity(), list));
    }
    private final ActivityResultLauncher<Intent> mapsActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                complaintDialogBinding.crimeLocation.setText(result.getData().getStringExtra("address"));
                selectedCrimeLatitude = result.getData().getDoubleExtra("latitude", 0);
                selectedCrimeLongitude = result.getData().getDoubleExtra("longitude", 0);
            }
        }
    });

    private ActivityResultLauncher<Intent> getContent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {

                evidenceUri = result.getData().getData();
                long fileSize = getFileSize(evidenceUri);

                if (fileSize <= EVIDENCE_FILE_SIZE_LIMIT) {
                    String selectedFileName = getFileName(evidenceUri);
                    complaintDialogBinding.crimeEvidence.setText(selectedFileName);
                } else {
                    Toast.makeText(getActivity(), "File size exceeds the limit (5MB)", Toast.LENGTH_SHORT).show();
                }

            } else {

            }
        }
    });

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private long getFileSize(Uri uri) {
        try (Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                return cursor.getLong(sizeIndex);
            }
        } catch (Exception e) {
            Log.e("FilePickerActivity", "Error getting file size", e);
        }
        return 0;
    }

    private void pickEvidence() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        getContent.launch(intent);
    }


    private void fetchComplaintsData() {

        binding.complaintsProgressbar.setVisibility(View.VISIBLE);
        complaints.clear();
        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("complaintList").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (getActivity() == null) {
                    return;
                }

                if (snapshot.exists()) {
                    complaints.clear(); // Clear complaints to avoid duplication
                    for (DataSnapshot snapshotData : snapshot.getChildren()) {
                        String complaintID = snapshotData.getValue(String.class);
                        listenForComplaintUpdates(complaintID);
                    }
                } else {
                    binding.complaintsProgressbar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.complaintsProgressbar.setVisibility(View.GONE);
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForComplaintUpdates(String complaintID) {
        complaintsRef.child(complaintID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ComplaintModel complaint = snapshot.getValue(ComplaintModel.class);
                    if (complaint != null) {
                        int index = findComplaintIndex(complaintID);
                        if (index == -1) {
                            // New complaint
                            complaints.add(complaint);
                        } else {
                            // Existing complaint, update it
                            complaints.set(index, complaint);
                        }

                        complaints.sort((complaint1, complaint2) -> {
                            return complaint2.getComplaintDateTime().compareTo(complaint1.getComplaintDateTime()); // Descending order
                        });
                        setDataToRecycler(complaints);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.complaintsProgressbar.setVisibility(View.GONE);
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int findComplaintIndex(String complaintID) {
        for (int i = 0; i < complaints.size(); i++) {
            if (complaints.get(i).getComplaintId().equals(complaintID)) {
                return i;
            }
        }
        return -1;
    }
    private void setDataToRecycler(List<ComplaintModel> complaints) {
        complaintsAdapter = new ComplaintsAdapter(getActivity(), complaints);
        binding.complaintsRecycler.setAdapter(complaintsAdapter);
        binding.complaintsProgressbar.setVisibility(View.GONE);
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

    private void startVoiceRecognitionTranslation(RecognitionCallback callback) {

        this.recognitionCallback = callback;

        // Start Speech Recognition Intent
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK"); // Specify the language, for example, en-US for English
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something...");

        voiceRecognitionTranslationResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> voiceRecognitionTranslationResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                ArrayList<String> recognizedResult = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//              LanguageTranslator to translate the recognized text
                LanguageTranslator.translateText(recognizedResult.get(0), getActivity(), new TranslationCallback() {
                    @Override
                    public void onTranslationComplete(String translatedText) {
                        if (recognitionCallback != null) {
                            recognitionCallback.onRecognitionComplete(translatedText);
                        }
                    }

                    @Override
                    public void onTranslationFailure(String errorMessage) {
                        if (recognitionCallback != null) {
                            recognitionCallback.onRecognitionFailure(errorMessage);
                        }
                    }
                });

                recognizedResult.clear();
            }
        }
    });

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