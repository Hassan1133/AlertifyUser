package com.example.alertify_user.interfaces;

public interface RecognitionCallback {
    void onRecognitionComplete(String result);

    void onRecognitionFailure(String errorMessage);
}

