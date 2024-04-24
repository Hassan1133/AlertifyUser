package com.example.alertify_user.activities;

import static com.example.alertify_user.constants.Constants.USERS_COMPLAINTS_REF;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.alertify_user.R;
import com.example.alertify_user.constants.Constants;
import com.example.alertify_user.databinding.ActivityComplaintsDetailsBinding;
import com.example.alertify_user.databinding.ComplaintDialogBinding;
import com.example.alertify_user.databinding.ComplaintFeedbackDialogBinding;
import com.example.alertify_user.models.ComplaintModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class ComplaintsDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityComplaintsDetailsBinding binding;
    private ComplaintModel complaintModel;
    private String evidenceUrl, evidenceType;
    private Dialog feedBackDialog;
    private DatabaseReference complaintsRef;
    private ComplaintFeedbackDialogBinding complaintFeedbackDialogBinding;

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
        complaintsRef = FirebaseDatabase.getInstance().getReference(USERS_COMPLAINTS_REF);
        getDataFromIntent();
    }

    void getDataFromIntent() {
        complaintModel = (ComplaintModel) getIntent().getSerializableExtra("complaintModel");
        evidenceUrl = complaintModel.getEvidenceUrl();
        evidenceType = complaintModel.getEvidenceType();
        binding.detailsCrimeType.setText(complaintModel.getCrimeType());
        binding.detailsCrime.setText(complaintModel.getCrimeDetails());
        binding.detailsCrimeLocation.setText(complaintModel.getCrimeLocation());
        binding.detailsCrimeDateTime.setText(String.format("%s %s", complaintModel.getCrimeDate(), complaintModel.getCrimeTime()));
        binding.detailsComplaintPoliceStation.setText(complaintModel.getPoliceStation());
        binding.detailsComplaintDateTime.setText(complaintModel.getComplaintDateTime());
        binding.complaintInvestigationStatus.setText(complaintModel.getInvestigationStatus());
    }

    @SuppressLint("NonConstantResourceId")
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

        if(evidenceUrl != null && !evidenceUrl.isEmpty() && evidenceType != null && !evidenceType.isEmpty())
        {

            String fileName = "evidence_file."+getFileType();

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
        complaintFeedbackDialogBinding = ComplaintFeedbackDialogBinding.inflate(LayoutInflater.from(ComplaintsDetailsActivity.this));
        feedBackDialog = new Dialog(ComplaintsDetailsActivity.this);
        feedBackDialog.setContentView(complaintFeedbackDialogBinding.getRoot());
        feedBackDialog.show();
        feedBackDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        complaintFeedbackDialogBinding.reportFeedback.setText(complaintModel.getFeedback());

        complaintFeedbackDialogBinding.complaintFeedbackLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!complaintFeedbackDialogBinding.reportFeedback.getText().toString().isEmpty() && complaintModel!=null)
                {
                    sendFeedback(complaintFeedbackDialogBinding.reportFeedback.getText().toString());
                }

                else {
                    complaintFeedbackDialogBinding.reportFeedback.setError("Please enter feedback");
                }
            }
        });
    }

    private void sendFeedback(String feedback) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("feedback", feedback);

        complaintsRef.child(complaintModel.getComplaintId()).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(ComplaintsDetailsActivity.this, "Feedback sent successfully", Toast.LENGTH_SHORT).show();
                feedBackDialog.dismiss();
                complaintModel.setFeedback(feedback);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ComplaintsDetailsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getFileType()
    {
        String fileType = "";
        if(evidenceType.matches("application/msword"))
        {
            fileType = "docx";
        }
        if(evidenceType.matches("application/pdf"))
        {
            fileType = "pdf";
        }
        if(evidenceType.matches("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
        {
            fileType = "pptx";
        }
        if(evidenceType.startsWith("video"))
        {
            fileType = "mp4";
        }
        if(evidenceType.startsWith("audio"))
        {
            fileType = "mp3";
        }
        if(evidenceType.startsWith("image"))
        {
            fileType = "jpg";
        }
        if(evidenceType.matches("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        {
            fileType = "xlsx";
        }
        return fileType;
    }
}