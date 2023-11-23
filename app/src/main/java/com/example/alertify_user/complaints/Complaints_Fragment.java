package com.example.alertify_user.complaints;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alertify_user.R;
import com.example.alertify_user.adapter.ComplaintsAdapter;
import com.example.alertify_user.adapter.DropDownAdapter;
import com.example.alertify_user.main_utils.LocationPermissionUtils;
import com.example.alertify_user.main_utils.MapsActivity;
import com.example.alertify_user.main_utils.NetworkUtils;
import com.example.alertify_user.model.ComplaintModel;
import com.example.alertify_user.model.CrimesModel;
import com.example.alertify_user.model.PoliceStationModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kunzisoft.switchdatetime.SwitchDateTimeDialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Complaints_Fragment extends Fragment implements View.OnClickListener {

    private static final int EARTH_RADIUS_KM = 6371; // Earth's radius in kilometers
    private FloatingActionButton addComplaintBtn;
    private Dialog complaintDialog;
    private Uri uriData;
    private ImageView evidenceImage;
    private VideoView evidenceVideo;
    private String imageSize;
    private String nearestPoliceStationName;
    private TextInputEditText crimeDetails, crimeLocation, crimeDateTime;
    private TextInputLayout crimeLocationLayout, crimeDateTimeLayout;
    private DropDownAdapter dropDownAdapter;
    private DatabaseReference crimesRef;
    private ArrayList<String> crimesList;
    private AutoCompleteTextView crimeType;
    private double selectedCrimeLatitude, selectedCrimeLongitude;
    private DatabaseReference policeStationsRef;
    private ArrayList<PoliceStationModel> policeStations;
    private ComplaintModel complaintModel;
    private LocationPermissionUtils permissionUtils;
    private StorageReference firebaseStorageReference;
    private DatabaseReference complaintsRef;
    private SwitchDateTimeDialogFragment dateTimeFragment;
    private static final String TAG = "Complaints_Fragment";
    private static final String TAG_DATETIME_FRAGMENT = "TAG_DATETIME_FRAGMENT";
    private ProgressBar complaintDialogProgressBar, complaintFragmentProgressBar;
    private List<ComplaintModel> complaints;
    private ComplaintsAdapter complaintsAdapter;
    private RecyclerView recyclerView;

    private String uriType;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.complaints_fragment, container, false);
        init(view);
        return view;
    }

    private void init(View view) {
        addComplaintBtn = view.findViewById(R.id.addBtn);
        addComplaintBtn.setOnClickListener(this);

        crimesRef = FirebaseDatabase.getInstance().getReference("AlertifyCrimes");
        crimesList = new ArrayList<>();

        policeStationsRef = FirebaseDatabase.getInstance().getReference("AlertifyPoliceStations");
        policeStations = new ArrayList<>();

        permissionUtils = new LocationPermissionUtils(getActivity());

        complaintsRef = FirebaseDatabase.getInstance().getReference("AlertifyUserComplaints"); // firebase initialization

        firebaseStorageReference = FirebaseStorage.getInstance().getReference();

        dateAndTimePicker();

        complaints = new ArrayList<ComplaintModel>();

        recyclerView = view.findViewById(R.id.complaints_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));

        complaintFragmentProgressBar = view.findViewById(R.id.complaints_fragment_progressbar);

        fetchComplaintsData();
    }

    // Method for date and time picker dialog
    private void dateAndTimePicker() {
        // Construct SwitchDateTimePicker
        dateTimeFragment = (SwitchDateTimeDialogFragment) getActivity().getSupportFragmentManager().findFragmentByTag(TAG_DATETIME_FRAGMENT);
        if (dateTimeFragment == null) {
            dateTimeFragment = SwitchDateTimeDialogFragment.newInstance(
                    getString(R.string.label_datetime_dialog),
                    getString(android.R.string.ok),
                    getString(android.R.string.cancel),
                    getString(R.string.clean),// Optional
                    "en"
            );
        }

        // Optionally define a timezone
        dateTimeFragment.setTimeZone(TimeZone.getDefault());

        // Init format
        final SimpleDateFormat myDateFormat = new SimpleDateFormat("d MMM yyyy hh:mm a", java.util.Locale.getDefault());
        // Assign unmodifiable values
        dateTimeFragment.set24HoursMode(false);
        dateTimeFragment.setHighlightAMPMSelection(false);

        // Define new day and month format
        try {
            dateTimeFragment.setSimpleDateMonthAndDayFormat(new SimpleDateFormat("MMMM dd", Locale.getDefault()));
        } catch (SwitchDateTimeDialogFragment.SimpleDateMonthAndDayFormatException e) {
            Log.e(TAG, e.getMessage());
        }

        // Set listener for date
        // Or use dateTimeFragment.setOnButtonClickListener(new SwitchDateTimeDialogFragment.OnButtonClickListener() {
        dateTimeFragment.setOnButtonClickListener(new SwitchDateTimeDialogFragment.OnButtonWithNeutralClickListener() {
            @Override
            public void onPositiveButtonClick(Date date) {
                crimeDateTime.setText(myDateFormat.format(date));
            }

            @Override
            public void onNegativeButtonClick(Date date) {
                // Do nothing
            }

            @Override
            public void onNeutralButtonClick(Date date) {
                // Optional if neutral button does not exists
                crimeDateTime.setText("");
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addBtn:
                if (NetworkUtils.isInternetAvailable(getActivity())) {
                    // Internet is available, call the method for creating dialog
                    createComplaintDialog();

                } else {
                    // Internet is not available, show a message to the user
                    Toast.makeText(getActivity(), "Please turn on your internet.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void createComplaintDialog() {
        complaintDialog = new Dialog(getActivity());
        complaintDialog.setContentView(R.layout.complaint_dialog);
        complaintDialog.show();
        complaintDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        evidenceImage = complaintDialog.findViewById(R.id.evidence_image);

        evidenceVideo = complaintDialog.findViewById(R.id.evidence_video);

        crimeType = complaintDialog.findViewById(R.id.crime_type);

        crimeDetails = complaintDialog.findViewById(R.id.crime_details);

        crimeLocation = complaintDialog.findViewById(R.id.crime_location);

        crimeLocationLayout = complaintDialog.findViewById(R.id.crime_location_layout);

        crimeDateTime = complaintDialog.findViewById(R.id.crime_date);

        crimeDateTimeLayout = complaintDialog.findViewById(R.id.crime_date_time_layout);

        complaintDialogProgressBar = complaintDialog.findViewById(R.id.complaint_dialog_progressbar);

        fetchCrimeTypeForDropDown();

        dropDownAdapter = new DropDownAdapter(getActivity(), crimesList);
        crimeType.setAdapter(dropDownAdapter);

        crimeLocationLayout.setEndIconOnClickListener(new View.OnClickListener() {
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

        crimeDateTimeLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateTimeFragment.startAtCalendarView();
                dateTimeFragment.setDefaultDateTime(Calendar.getInstance().getTime());
                dateTimeFragment.show(getActivity().getSupportFragmentManager(), TAG_DATETIME_FRAGMENT);
            }

        });

        evidenceImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImageOrVideo();
            }
        });

        complaintDialog.findViewById(R.id.evidence_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImageOrVideo();
            }
        });
        complaintDialog.findViewById(R.id.close_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                complaintDialog.dismiss();
            }
        });
        complaintDialog.findViewById(R.id.report_crime_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDataValid()) {
                    complaintDialogProgressBar.setVisibility(View.VISIBLE);
                    getPoliceStationsData();
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
                    getNearestPoliceStation();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
                complaintDialogProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private double calculateDistance(double userLat, double userLong, double policeStationLat, double policeStationLong) {
        double userLatitude = Math.toRadians(userLat);
        double userLongitude = Math.toRadians(userLong);
        double policeStationLatitude = Math.toRadians(policeStationLat);
        double policeStationLongitude = Math.toRadians(policeStationLong);

        double dLat = userLatitude - policeStationLatitude;
        double dLon = userLongitude - policeStationLongitude;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(userLatitude) * Math.cos(policeStationLatitude) * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private void getNearestPoliceStation() {// Find the nearest police station
        if (policeStations != null && !policeStations.isEmpty()) { // Check if the list is not null and not empty
            PoliceStationModel nearestPoliceStation = null;
            double minDistance = Double.MAX_VALUE;

            for (PoliceStationModel policeStation : policeStations) {

                // Calculate distance using the Haversine formula
                double distance = calculateDistance(selectedCrimeLatitude, selectedCrimeLongitude, policeStation.getPoliceStationLatitude(), policeStation.getPoliceStationLongitude());

                if (distance < minDistance) {
                    minDistance = distance;
                    nearestPoliceStation = policeStation;
                }
            }

            if (nearestPoliceStation != null) {
                // 'nearestPoliceStation' now contains the nearest police station to the user's location
                nearestPoliceStationName = nearestPoliceStation.getPoliceStationName();

                addDataToModel();
            }
        }
    }

    private void addDataToModel() {
        complaintModel = new ComplaintModel();
        complaintModel.setCrimeType(crimeType.getText().toString());
        complaintModel.setCrimeDetails(crimeDetails.getText().toString().trim());
        complaintModel.setCrimeDateTime(crimeDateTime.getText().toString());
        complaintModel.setComplaintDateTime(getCurrentDateTime());
        complaintModel.setCrimeLatitude(selectedCrimeLatitude);
        complaintModel.setCrimeLongitude(selectedCrimeLongitude);
        complaintModel.setCrimeLocation(crimeLocation.getText().toString());
        complaintModel.setPoliceStation(nearestPoliceStationName);
        complaintModel.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        complaintModel.setInvestigationStatus("Reported");
        complaintModel.setFeedback("none");
        uploadEvidence(complaintModel);
    }

    private String getCurrentDateTime() {
        // Get the current date and time
        Date currentDate = new Date();

        // Create a SimpleDateFormat to format the date and time as desired
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy hh:mm a");
        String formattedDate = dateFormat.format(currentDate);
        return formattedDate;
    }

    private void uploadEvidence(ComplaintModel complaintModel) {
        complaintModel.setComplaintId(complaintsRef.push().getKey());

        StorageReference strRef = firebaseStorageReference.child("Alertify_Complaints_Evidences/" + complaintModel.getComplaintId());
        strRef.putFile(uriData).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                strRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        complaintModel.setEvidenceUrl(task.getResult().toString());
                        complaintModel.setEvidenceType(uriType);
                        addToDb(complaintModel);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        complaintDialogProgressBar.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                complaintDialogProgressBar.setVisibility(View.INVISIBLE);
            }
        });
}

    private void addToDb(ComplaintModel complaintModel) {
        complaintsRef.child(complaintModel.getComplaintId()).setValue(complaintModel).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    complaintDialogProgressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(getActivity(), "Complaint Reported successfully!", Toast.LENGTH_SHORT).show();
                    complaintDialog.dismiss();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                complaintDialogProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private boolean isDataValid() {
        boolean valid = true;

        if (uriData == null) {
            Toast.makeText(getActivity(), "Please select evidence", Toast.LENGTH_SHORT).show();
            valid = false;
        }
        if (crimeType.getText().length() == 0) {
            crimeType.setError("Please select crime type");
            valid = false;
        }
        if (crimeDetails.getText().length() == 0) {
            crimeDetails.setError("Please enter crime details");
            valid = false;
        }
        if (crimeDateTime.getText().length() == 0) {
            Toast.makeText(getActivity(), "Please select crime date and time", Toast.LENGTH_SHORT).show();
            valid = false;
        }
        if (crimeLocation.getText().length() == 0) {
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final ActivityResultLauncher<Intent> mapsActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                crimeLocation.setText(result.getData().getStringExtra("address"));
                selectedCrimeLatitude = result.getData().getDoubleExtra("latitude", 0);
                selectedCrimeLongitude = result.getData().getDoubleExtra("longitude", 0);
            }
        }
    });

    private void startVideoPlayback(Uri videoUri) {
        evidenceImage.setVisibility(View.INVISIBLE);
        evidenceVideo.setVisibility(View.VISIBLE);

        evidenceVideo.setVideoURI(videoUri);
        evidenceVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                // This callback is triggered when the video is prepared for playback
                mp.setVolume(0, 0); // Mute the video
                evidenceVideo.start(); // Start video playback
            }
        });
    }

    private ActivityResultLauncher<Intent> getContent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {

                Uri uri = result.getData().getData();
                uriType = getActivity().getContentResolver().getType(uri);

                if (uriType.startsWith("image/")) {

                    if (uri != null) {

                        if (isImageSizeValid(uri)) {

                            uriData = uri;

                            evidenceVideo.setVisibility(View.INVISIBLE);
                            evidenceImage.setVisibility(View.VISIBLE);
                            evidenceImage.setImageURI(uriData);

                        } else {
                            Toast.makeText(getActivity(), imageSize + ". Please select an image smaller than 2 MB", Toast.LENGTH_SHORT).show();
                        }

                    }

                } else if (uriType.startsWith("video/")) {

                    MediaPlayer mp = MediaPlayer.create(getActivity(), uri);
                    int duration = mp.getDuration();

                    if ((duration / 1000) < 60) {

                        uriData = uri;

                        startVideoPlayback(uriData);
                        Toast.makeText(getActivity(), uriType, Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(getActivity(), "your video is about " + (duration / 1000) + " seconds Please select video less than 60 seconds", Toast.LENGTH_SHORT).show();
                    }

                }
            } else {
                evidenceVideo.setVisibility(View.INVISIBLE);
                evidenceImage.setVisibility(View.VISIBLE);
            }
        }
    });


    private void pickImageOrVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/* video/*");
        getContent.launch(intent);
    }

    // Method to check if the selected image size is valid (less than 2MB)
    private boolean isImageSizeValid(Uri imageUri) {
        try {
            // Get the image file size in bytes
            long imageSizeInBytes = getImageSizeInBytes(imageUri);

            // Convert the size to MB
            double imageSizeInMB = imageSizeInBytes / (1024.0 * 1024.0);

            imageSize = String.format("Selected image size is %.2f MB", imageSizeInMB);

            // Compare with the 2MB limit
            return imageSizeInMB < 2.0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Method to get the image file size in bytes
    private long getImageSizeInBytes(Uri imageUri) throws Exception {
        Cursor cursor = getActivity().getContentResolver().query(imageUri, null, null, null, null);
        if (cursor == null) {
            throw new Exception("Cursor is null");
        }
        cursor.moveToFirst();
        long sizeInBytes = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
        cursor.close();
        return sizeInBytes;
    }
    private void fetchComplaintsData() {

        complaintFragmentProgressBar.setVisibility(View.VISIBLE);

        complaintsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                complaints.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    complaints.add(dataSnapshot.getValue(ComplaintModel.class));
                }

                complaintFragmentProgressBar.setVisibility(View.INVISIBLE);

                setDataToRecycler(complaints);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setDataToRecycler(List<ComplaintModel> complaints) {
        complaintsAdapter = new ComplaintsAdapter(getActivity(), complaints);
        recyclerView.setAdapter(complaintsAdapter);
    }
}