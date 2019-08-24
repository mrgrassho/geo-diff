package com.geodiff.dto;

public class GeoAsset {
    private String date;
    private Coordinate coordinate;

    public GeoAsset(String date, Coordinate coordinate) {
        this.date = date;
        this.coordinate = coordinate;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }
}
