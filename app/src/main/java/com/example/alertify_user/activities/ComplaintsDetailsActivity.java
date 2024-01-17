package com.example.alertify_user.activities;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.alertify_user.R;
import com.example.alertify_user.models.ComplaintModel;
import com.google.android.material.textfield.TextInputEditText;

public class ComplaintsDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView crimeType, crimeDetails, crimeLocation, crimeDateTime, complaintPoliceStation, complaintDateTime, feedBack;
    private ImageView evidenceImage, evidenceVideo;
    private ComplaintModel complaintModel;
    private String evidenceUrl;
    private RelativeLayout evidenceLayout;
    private Dialog feedBackDialog;

    private TextInputEditText feedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaints_details);

        init();
    }

    void init() {
        crimeType = findViewById(R.id.details_crime_type);
        crimeDetails = findViewById(R.id.details_crime);
        crimeLocation = findViewById(R.id.details_crime_location);
        crimeDateTime = findViewById(R.id.details_crime_date_time);
        complaintPoliceStation = findViewById(R.id.details_complaint_police_station);
        complaintDateTime = findViewById(R.id.details_complaint_date_time);
        evidenceImage = findViewById(R.id.crime_evidence_image);
        evidenceVideo = findViewById(R.id.crime_evidence_video);
        evidenceLayout = findViewById(R.id.crime_evidence_layout);
        evidenceLayout.setOnClickListener(this);
        feedBack = findViewById(R.id.feedback);
        feedBack.setOnClickListener(this);
        getDataFromIntent();
    }

    void getDataFromIntent() {
        complaintModel = (ComplaintModel) getIntent().getSerializableExtra("complaintModel");
        evidenceUrl = complaintModel.getEvidenceUrl();

//        if (complaintModel.getEvidenceType().startsWith("image/")) {
//
//            Glide.with(getApplicationContext()).load(evidenceUrl).into(evidenceImage);
//
//        } else if (complaintModel.getEvidenceType().startsWith("video/")) {
//            RequestOptions requestOptions = new RequestOptions();
//            Glide.with(ComplaintsDetailsActivity.this)
//                    .load(evidenceUrl)
//                    .apply(requestOptions)
//                    .thumbnail(Glide.with(ComplaintsDetailsActivity.this).load(evidenceUrl))
//                    .into(evidenceImage);
//            evidenceVideo.setVisibility(View.VISIBLE);
//        }

        crimeType.setText(complaintModel.getCrimeType());
        crimeDetails.setText(complaintModel.getCrimeDetails());
        crimeLocation.setText(complaintModel.getCrimeLocation());
        crimeDateTime.setText(complaintModel.getCrimeDateTime());
        complaintPoliceStation.setText(complaintModel.getPoliceStation());
        complaintDateTime.setText(complaintModel.getComplaintDateTime());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.crime_evidence_layout:
//                goToSeeEvidenceActivity(complaintModel);
                break;
            case R.id.feedback:
                createFeedBackDialog();
                break;
        }
    }

    private void createFeedBackDialog() {
        feedBackDialog = new Dialog(ComplaintsDetailsActivity.this);
        feedBackDialog.setContentView(R.layout.complaint_feedback_dialog);
        feedBackDialog.show();
        feedBackDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        feedback = feedBackDialog.findViewById(R.id.report_feedback);
        feedBack.setText(complaintModel.getFeedback());
    }

//    private void goToSeeEvidenceActivity(ComplaintModel complaintModel) {
//        Intent intent = new Intent(ComplaintsDetailsActivity.this, SeeEvidenceActivity.class);
//        intent.putExtra("evidenceUrl", complaintModel.getEvidenceUrl());
//        intent.putExtra("evidenceType", complaintModel.getEvidenceType());
//        startActivity(intent);
//    }
}