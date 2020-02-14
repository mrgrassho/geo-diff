package com.geodiff.model;

import com.nasa.model.earth.EarthImage;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class GeoImage {

    @Id
    private String Id;

    private EarthImage earthImage;

    private FilterOption filterOption;

    private String vectorImage;

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

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getVectorImage() {
        return vectorImage;
    }

    public void setVectorImage(String vectorImage) {
        this.vectorImage = vectorImage;
    }

}
