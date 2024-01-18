package com.example.alertify_user.fragments;

import static com.example.alertify_user.constants.Constants.EVIDENCE_FILE_SIZE_LIMIT;

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
import android.provider.OpenableColumns;
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
import com.example.alertify_user.adapters.ComplaintsAdapter;
import com.example.alertify_user.adapters.DropDownAdapter;
import com.example.alertify_user.databinding.ComplaintDialogBinding;
import com.example.alertify_user.databinding.ComplaintsFragmentBinding;
import com.example.alertify_user.main_utils.LatLngWrapper;
import com.example.alertify_user.main_utils.LoadingDialog;
import com.example.alertify_user.main_utils.LocationPermissionUtils;
import com.example.alertify_user.activities.MapsActivity;
import com.example.alertify_user.main_utils.NetworkUtils;
import com.example.alertify_user.models.ComplaintModel;
import com.example.alertify_user.models.CrimesModel;
import com.example.alertify_user.models.PoliceStationModel;
import com.google.android.gms.maps.model.LatLng;
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
import com.google.maps.android.PolyUtil;
import com.kunzisoft.switchdatetime.SwitchDateTimeDialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Complaints_Fragment extends Fragment implements View.OnClickListener {

    private ComplaintsFragmentBinding binding;
    private ComplaintDialogBinding complaintDialogBinding;
    private Dialog complaintDialog;
    private String appropriatePoliceStationName;
    private DropDownAdapter dropDownAdapter;
    private DatabaseReference crimesRef;
    private ArrayList<String> crimesList;
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
    private List<ComplaintModel> complaints;
    private ComplaintsAdapter complaintsAdapter;
    private Uri evidenceUri;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = ComplaintsFragmentBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }

    private void init() {
        binding.addComplaintBtn.setOnClickListener(this);

        crimesRef = FirebaseDatabase.getInstance().getReference("AlertifyCrimes");
        crimesList = new ArrayList<>();

        policeStationsRef = FirebaseDatabase.getInstance().getReference("AlertifyPoliceStations");
        policeStations = new ArrayList<>();

        permissionUtils = new LocationPermissionUtils(getActivity());

        complaintsRef = FirebaseDatabase.getInstance().getReference("AlertifyUserComplaints"); // firebase initialization

        firebaseStorageReference = FirebaseStorage.getInstance().getReference();

        dateAndTimePicker();

        complaints = new ArrayList<ComplaintModel>();

        binding.complaintsRecycler.setLayoutManager(new GridLayoutManager(getActivity(), 2));

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
                complaintDialogBinding.crimeDateTime.setText(myDateFormat.format(date));
            }

            @Override
            public void onNegativeButtonClick(Date date) {
                // Do nothing
            }

            @Override
            public void onNeutralButtonClick(Date date) {
                // Optional if neutral button does not exists
                complaintDialogBinding.crimeDateTime.setText("");
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addComplaintBtn:
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
        complaintDialogBinding = ComplaintDialogBinding.inflate(LayoutInflater.from(getActivity()));
        complaintDialog = new Dialog(getActivity());
        complaintDialog.setContentView(complaintDialogBinding.getRoot());
        complaintDialog.show();
        complaintDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        fetchCrimeTypeForDropDown();

        dropDownAdapter = new DropDownAdapter(getActivity(), crimesList);
        complaintDialogBinding.crimeType.setAdapter(dropDownAdapter);

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

        complaintDialogBinding.crimeDateTimeLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateTimeFragment.startAtCalendarView();
                dateTimeFragment.setDefaultDateTime(Calendar.getInstance().getTime());
                dateTimeFragment.show(getActivity().getSupportFragmentManager(), TAG_DATETIME_FRAGMENT);
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
                    LoadingDialog.showLoadingDialog(getActivity());
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
                    getAppropriatePoliceStation();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
                LoadingDialog.hideLoadingDialog();
            }
        });
    }

    private void getAppropriatePoliceStation() {// Find the appropriate police station
        if (policeStations != null && !policeStations.isEmpty()) { // Check if the list is not null and not empty


            for (PoliceStationModel policeStation : policeStations) {

                List<LatLng> latLngPoints = convertLatLngWrapperList(policeStation.getBoundaries());

                if (isLocationInsidePoliceStationArea(new LatLng(selectedCrimeLatitude, selectedCrimeLongitude), latLngPoints)) {
                    appropriatePoliceStationName = policeStation.getPoliceStationName();
                    addDataToModel();
                    break;
                }
            }

            if (appropriatePoliceStationName == null) {
                LoadingDialog.hideLoadingDialog();
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

    private void addDataToModel() {
        complaintModel = new ComplaintModel();
        complaintModel.setCrimeType(complaintDialogBinding.crimeType.getText().toString());
        complaintModel.setCrimeDetails(complaintDialogBinding.crimeDetails.getText().toString().trim());
        complaintModel.setCrimeDateTime(complaintDialogBinding.crimeDateTime.getText().toString());
        complaintModel.setComplaintDateTime(getCurrentDateTime());
        complaintModel.setCrimeLatitude(selectedCrimeLatitude);
        complaintModel.setCrimeLongitude(selectedCrimeLongitude);
        complaintModel.setCrimeLocation(complaintDialogBinding.crimeLocation.getText().toString());
        complaintModel.setPoliceStation(appropriatePoliceStationName);
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

        if (evidenceUri != null) {
            StorageReference strRef = firebaseStorageReference.child("Alertify_Complaints_Evidences/" + complaintModel.getComplaintId());
            strRef.putFile(evidenceUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    strRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            complaintModel.setEvidenceUrl(task.getResult().toString());
                            addToDb(complaintModel);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            LoadingDialog.hideLoadingDialog();
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    LoadingDialog.hideLoadingDialog();
                }
            });
        } else {
            complaintModel.setEvidenceUrl("");
            addToDb(complaintModel);
        }
    }

    private void addToDb(ComplaintModel complaintModel) {
        complaintsRef.child(complaintModel.getComplaintId()).setValue(complaintModel).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    LoadingDialog.hideLoadingDialog();
                    Toast.makeText(getActivity(), "Complaint Reported successfully!", Toast.LENGTH_SHORT).show();
                    complaintDialog.dismiss();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                LoadingDialog.hideLoadingDialog();
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
        if (complaintDialogBinding.crimeDateTime.getText().length() == 0) {
            Toast.makeText(getActivity(), "Please select crime date and time", Toast.LENGTH_SHORT).show();
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

        LoadingDialog.showLoadingDialog(getActivity());

        complaintsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                complaints.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    complaints.add(dataSnapshot.getValue(ComplaintModel.class));
                }

                LoadingDialog.hideLoadingDialog();

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
        binding.complaintsRecycler.setAdapter(complaintsAdapter);
    }
}