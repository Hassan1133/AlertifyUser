package com.example.alertify_user.models;

import java.io.Serializable;

public class LawsModel implements Serializable {
    private String crimeType;

    public String getCrimeType() {
        return crimeType;
    }

    public void setCrimeType(String crimeType) {
        this.crimeType = crimeType;
    }
}
