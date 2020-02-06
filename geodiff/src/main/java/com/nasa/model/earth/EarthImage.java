package com.nasa.model.earth;

import com.geodiff.dto.Coordinate;
import com.google.gson.annotations.SerializedName;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.Base64;

public class EarthImage {

    private String id;
    private String date;
    private String url;
    @SerializedName("cloud_score")
    private double cloudScore;
    private Coordinate coordinate;
    private Double dim;
    private String rawImage;

    public String getDate() {
        return date;
    }

    public String getUrl() {
        return url;
    }

    public double getCloudScore() {
        return cloudScore;
    }

    public String getId() {
        return id;
    }

    public void setImageByUrl(String url,OkHttpClient client) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = null;
            response = client.newCall(request).execute();
            this.setRawImage("data:image/png;base64," + Base64.getEncoder().encodeToString(response.body().bytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setImageByUrl(OkHttpClient client){
        setImageByUrl(this.url, client);
    }

    public void setRawImage(String rawImage) {
        this.rawImage = rawImage;
    }

    public String getRawImage() {
        return this.rawImage;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public Double getDim() {
        return dim;
    }

    public void setDim(Double dim) {
        this.dim = dim;
    }
}
