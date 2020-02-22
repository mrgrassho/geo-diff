package com.geodiff.model;

import org.springframework.data.annotation.Id;

public class FilteredImage {

    @Id
    private String Id;

    private String filterName;

    private String vectorImage;

    public FilteredImage () {}

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getVectorImage() {
        return vectorImage;
    }

    public void setVectorImage(String vectorImage) {
        this.vectorImage = vectorImage;
    }

}
