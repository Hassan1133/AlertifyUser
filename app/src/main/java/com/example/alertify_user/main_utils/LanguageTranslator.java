package com.example.alertify_user.main_utils;

import android.content.Context;
import androidx.annotation.NonNull;

import com.example.alertify_user.interfaces.TranslationCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public class LanguageTranslator {
    public static void translateText(String text, Context context, TranslationCallback callback) {
        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.URDU)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build();

        final Translator englishGermanTranslator =
                Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        englishGermanTranslator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {

                englishGermanTranslator.translate(text).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        callback.onTranslationComplete(s);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onTranslationFailure(e.toString());
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.onTranslationFailure(e.toString());
            }
        });
    }
}
