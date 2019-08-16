package com.nasa.model.earth;

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
