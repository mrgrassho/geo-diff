package com.geodiff.model;

import com.nasa.model.earth.EarthImage;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Document
public class GeoImage {

    @Id
    private String Id;

    private EarthImage earthImage;

    private ArrayList<FilteredImage> filteredImages;

    public GeoImage () {
        filteredImages = new ArrayList<>();
    }

    public EarthImage getEarthImage() {
        return earthImage;
    }

    public void setEarthImage(EarthImage earthImage) {
        this.earthImage = earthImage;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public ArrayList<FilteredImage> getfilteredImages(){
        return filteredImages;
    }

    public void setfilteredImages(ArrayList<FilteredImage>  filteredImages){
        this.filteredImages = filteredImages;
    }
}
