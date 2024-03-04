package com.example.alertify_user.models;

public class EmergencyServiceModel {

    private String requestId;
    private String requestStatus;
    private String userId;
    private String userEmail;
    private String userName;
    private String userCnic;
    private String userPhoneNo;
    private String policeStation;
    private String requestDateTime;
    private double userCurrentLatitude;
    private double userCurrentLongitude;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserCnic() {
        return userCnic;
    }

    public void setUserCnic(String userCnic) {
        this.userCnic = userCnic;
    }

    public String getUserPhoneNo() {
        return userPhoneNo;
    }

    public void setUserPhoneNo(String userPhoneNo) {
        this.userPhoneNo = userPhoneNo;
    }

    public String getPoliceStation() {
        return policeStation;
    }

    public void setPoliceStation(String policeStation) {
        this.policeStation = policeStation;
    }

    public double getUserCurrentLatitude() {
        return userCurrentLatitude;
    }

    public void setUserCurrentLatitude(double userCurrentLatitude) {
        this.userCurrentLatitude = userCurrentLatitude;
    }

    public double getUserCurrentLongitude() {
        return userCurrentLongitude;
    }

    public void setUserCurrentLongitude(double userCurrentLongitude) {
        this.userCurrentLongitude = userCurrentLongitude;
    }

    public String getRequestDateTime() {
        return requestDateTime;
    }

    public void setRequestDateTime(String requestDateTime) {
        this.requestDateTime = requestDateTime;
    }
}
