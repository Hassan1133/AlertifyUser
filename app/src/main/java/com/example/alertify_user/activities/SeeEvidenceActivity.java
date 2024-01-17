package com.example.alertify_user.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.example.alertify_user.R;
import com.jsibbold.zoomage.ZoomageView;

public class SeeEvidenceActivity extends AppCompatActivity {

    private ZoomageView evidenceImage;
    private PlayerView evidenceVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_evidence);
        init();
    }

    private void init() {
        evidenceImage = findViewById(R.id.see_evidence_image);
        evidenceVideo = findViewById(R.id.see_evidence_video);

        getIntentData();
    }

    private void getIntentData() {
        String evidenceType = getIntent().getStringExtra("evidenceType");
        String evidenceUrl = getIntent().getStringExtra("evidenceUrl");

        if (evidenceType.startsWith("image/")) {

            Glide.with(getApplicationContext()).load(evidenceUrl).into(evidenceImage);

        } else if (evidenceType.startsWith("video/")) {
            evidenceImage.setVisibility(View.GONE);
            evidenceVideo.setVisibility(View.VISIBLE);
            startVideoPlayback(Uri.parse(evidenceUrl));
        }
    }

    private void startVideoPlayback(Uri videoUri) {

        ExoPlayer player = new ExoPlayer.Builder(SeeEvidenceActivity.this).build();

        evidenceVideo.setPlayer(player);

        // Build the media item.
        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        // Set the media item to be played.
        player.setMediaItem(mediaItem);
        // Prepare the player.
        player.prepare();
        // Start the playback.
        player.play();
    }
}