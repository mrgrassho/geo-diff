package com.nasa.model.earth;

import com.geodiff.dto.Coordinate;

import java.util.List;

public final class EarthAssets {

    private int count;
    private List<EarthAsset> results;

    public int getCount() {
        return count;
    }

    public List<EarthAsset> getResults() {
        return results;
    }

    private Coordinate coordinate;

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public static final class EarthAsset {

        private String date;
        private String id;


        public String getDate() {
            return date;
        }

        public String getId() {
            return id;
        }

    }
}
