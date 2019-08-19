package com.geodiff.model;

import com.nasa.model.earth.EarthImage;

public class GeoImage {

    private Long Id;

    private EarthImage earthImage;

    private FilterOption filterOption;

    public GeoImage () {}

    public EarthImage getEarthImage() {
        return earthImage;
    }

    public void setEarthImage(EarthImage earthImage) {
        this.earthImage = earthImage;
    }

    public FilterOption getFilterOption() {
        return filterOption;
    }

    public void setFilterOption(FilterOption filterOption) {
        this.filterOption = filterOption;
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }
}
