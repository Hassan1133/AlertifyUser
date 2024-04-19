package com.example.alertify_user.models;

import java.io.Serializable;

public class CriminalsModel implements Serializable {
    private String criminalCnic;
    private String criminalName;

    public String getCriminalCnic() {
        return criminalCnic;
    }

    public void setCriminalCnic(String criminalCnic) {
        this.criminalCnic = criminalCnic;
    }

    public String getCriminalName() {
        return criminalName;
    }

    public void setCriminalName(String criminalName) {
        this.criminalName = criminalName;
    }
}
