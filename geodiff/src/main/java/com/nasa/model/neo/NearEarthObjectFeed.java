package com.nasa.model.neo;

import com.google.gson.annotations.SerializedName;
import com.nasa.model.Links;

import java.util.List;
import java.util.Map;

public final class NearEarthObjectFeed {

    private Links links;
    @SerializedName("element_count")
    private int elementCount;
    @SerializedName("near_earth_objects")
    private Map<String, List<NearEarthObject>> nearEarthObjects;

    public Links getLinks() {
        return links;
    }

    public int getElementCount() {
        return elementCount;
    }

    public Map<String, List<NearEarthObject>> getNearEarthObjects() {
        return nearEarthObjects;
    }
}
