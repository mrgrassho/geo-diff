package com.geodiff.dto;

import java.util.Locale;

public class Coordinate {
    private Double latitude;
    private Double longitude;

    public Coordinate() { }

    public Coordinate(Double latitude,Double longitude){

        this.latitude = Double.parseDouble(String.format(Locale.US,"%.3f", latitude));
        this.longitude = Double.parseDouble(String.format(Locale.US,"%.3f", longitude));
        //this.latitude = latitude;
        //this.longitude = longitude;

    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "["+getLatitude()+","+getLongitude()+"]";
    }
}
