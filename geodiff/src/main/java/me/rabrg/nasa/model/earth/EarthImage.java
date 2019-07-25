package me.rabrg.nasa.model.earth;

import com.google.gson.annotations.SerializedName;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class EarthImage {

    private String date;
    private String url;
    @SerializedName("cloud_score")
    private double cloudScore;
    private String id;
    private byte[] rawImage;

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
            this.setRawImage(response.body().bytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setImageByUrl(OkHttpClient client){
        setImageByUrl(this.url, client);
    }

    public void setRawImage(byte[] rawImage) {
        this.rawImage = rawImage;
    }

    public byte[] getRawImage() {
        return this.rawImage;
    }
}
