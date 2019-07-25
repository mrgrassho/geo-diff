package com.geodiff.dto;

public class Coordinate {
    private Double latitude;
    private Double longitude;

    public Coordinate() { }

    public Coordinate(Double latitude,Double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
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
        return "(lat:" + getLatitude() + ", lon:" + getLongitude().toString() +")";
    }
}
