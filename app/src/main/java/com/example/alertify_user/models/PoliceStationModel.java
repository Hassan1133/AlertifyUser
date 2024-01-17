package com.example.alertify_user.models;

import com.example.alertify_user.main_utils.LatLngWrapper;

import java.io.Serializable;
import java.util.List;

public class PoliceStationModel implements Serializable {
    private String id;
    private String policeStationName;
    private String policeStationLocation;
    private double policeStationLatitude;
    private double policeStationLongitude;

    private String policeStationNumber;
    private String imgUrl;

    private List<LatLngWrapper> boundaries; // List of LatLng for boundaries

    public String getPoliceStationNumber() {
        return policeStationNumber;
    }

    public void setPoliceStationNumber(String policeStationNumber) {
        this.policeStationNumber = policeStationNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPoliceStationName() {
        return policeStationName;
    }

    public void setPoliceStationName(String policeStationName) {
        this.policeStationName = policeStationName;
    }

    public String getPoliceStationLocation() {
        return policeStationLocation;
    }

    public void setPoliceStationLocation(String policeStationLocation) {
        this.policeStationLocation = policeStationLocation;
    }

    public double getPoliceStationLatitude() {
        return policeStationLatitude;
    }

    public void setPoliceStationLatitude(double policeStationLatitude) {
        this.policeStationLatitude = policeStationLatitude;
    }

    public double getPoliceStationLongitude() {
        return policeStationLongitude;
    }

    public void setPoliceStationLongitude(double policeStationLongitude) {
        this.policeStationLongitude = policeStationLongitude;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public List<LatLngWrapper> getBoundaries() {
        return boundaries;
    }
    public void setBoundaries(List<LatLngWrapper> boundaries) {
        this.boundaries = boundaries;
    }

}