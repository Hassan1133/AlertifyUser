package com.example.alertify_user.interfaces;

public interface TranslationCallback {
    void onTranslationComplete(String translatedText);

    void onTranslationFailure(String errorMessage);
}