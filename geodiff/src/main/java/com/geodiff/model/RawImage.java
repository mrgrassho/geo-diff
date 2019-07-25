package com.geodiff.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.geodiff.dto.Coordinate;
import com.querydsl.core.annotations.QueryEntity;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.util.Date;

@QueryEntity
@Document
public class RawImage {

    @Id
    private Long Id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'hh:mm:ss")
    private Date date;

    private Coordinate coordinate;

    private String url;

    private Double cloudScore;

    private byte[] rawImage;

    public RawImage() {}

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }


    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Double getCloudScore() {
        return cloudScore;
    }

    public void setCloudScore(Double cloudScore) {
        this.cloudScore = cloudScore;
    }

    public byte[] getRawImage() {
        return rawImage;
    }

    public void setImageByUrl(String url) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                .url(url)
                .build();
            Response response = null;
            response = client.newCall(request).execute();
            this.setRawImage(response.body().bytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRawImage(byte[] rawImage) {
        this.rawImage = rawImage;
    }
}
