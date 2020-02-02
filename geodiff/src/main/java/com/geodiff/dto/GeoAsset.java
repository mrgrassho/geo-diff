package com.geodiff.dto;

public class GeoAsset {
    private String date;
    private Coordinate centerCoordinate;

    public GeoAsset(String date, Coordinate centerCoordinate) {
        this.date = date;
        this.centerCoordinate = centerCoordinate;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Coordinate getCenterCoordinate() {
        return centerCoordinate;
    }

    public void setCenterCoordinate(Coordinate centerCoordinate) {
        this.centerCoordinate = centerCoordinate;
    }
}
