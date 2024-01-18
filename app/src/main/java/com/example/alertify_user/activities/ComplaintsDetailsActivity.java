package com.example.alertify_user.activities;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.alertify_user.R;
import com.example.alertify_user.databinding.ActivityComplaintsDetailsBinding;
import com.example.alertify_user.models.ComplaintModel;
import com.google.android.material.textfield.TextInputEditText;

public class ComplaintsDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityComplaintsDetailsBinding binding;
    private ComplaintModel complaintModel;
    private String evidenceUrl;
    private Dialog feedBackDialog;

    private TextInputEditText feedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComplaintsDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
    }

    void init() {
        binding.feedBack.setOnClickListener(this);
        binding.downloadEvidenceBtn.setOnClickListener(this);
        getDataFromIntent();
    }

    void getDataFromIntent() {
        complaintModel = (ComplaintModel) getIntent().getSerializableExtra("complaintModel");
        evidenceUrl = complaintModel.getEvidenceUrl();
        binding.detailsCrimeType.setText(complaintModel.getCrimeType());
        binding.detailsCrime.setText(complaintModel.getCrimeDetails());
        binding.detailsCrimeLocation.setText(complaintModel.getCrimeLocation());
        binding.detailsCrimeDateTime.setText(complaintModel.getCrimeDateTime());
        binding.detailsComplaintPoliceStation.setText(complaintModel.getPoliceStation());
        binding.detailsComplaintDateTime.setText(complaintModel.getComplaintDateTime());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.feedBack:
                createFeedBackDialog();
                break;
            case R.id.downloadEvidenceBtn:
                downloadEvidence();
                break;
        }
    }

    private void downloadEvidence() {

        if(evidenceUrl != null && !evidenceUrl.isEmpty())
        {
            String fileName = "evidence_file";

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(evidenceUrl))
                    .setTitle("File Download") // Title of the notification during download
                    .setDescription("Downloading") // Description of the notification during download
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(ComplaintsDetailsActivity.this, "Download started", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ComplaintsDetailsActivity.this, "Download Manager not available", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(ComplaintsDetailsActivity.this, "Evidence not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void createFeedBackDialog() {
        feedBackDialog = new Dialog(ComplaintsDetailsActivity.this);
        feedBackDialog.setContentView(R.layout.complaint_feedback_dialog);
        feedBackDialog.show();
        feedBackDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        feedback = feedBackDialog.findViewById(R.id.report_feedback);
        binding.feedBack.setText(complaintModel.getFeedback());
    }
}