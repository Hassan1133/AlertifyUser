package com.example.alertify_user.models;

public class CriminalCrimesModel {
    private String FIRNumber;
    private String PoliceStation;
    private String District;
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFIRNumber() {
        return FIRNumber;
    }

    public void setFIRNumber(String FIRNumber) {
        this.FIRNumber = FIRNumber;
    }

    public String getPoliceStation() {
        return PoliceStation;
    }

    public void setPoliceStation(String policeStation) {
        PoliceStation = policeStation;
    }

    public String getDistrict() {
        return District;
    }

    public void setDistrict(String district) {
        District = district;
    }
}
