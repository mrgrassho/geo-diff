package com.nasa.service;

import com.nasa.model.neo.NearEarthObject;
import com.nasa.model.neo.NearEarthObjectBrowse;
import com.nasa.model.neo.NearEarthObjectFeed;
import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

public interface NearEarthObjectService {

    @GET("/neo/reset/v1/feed")
    Call<NearEarthObjectFeed> feed(@Query("start_date") String startDate, @Query("end_date") String endDate,
                                   @Query("api_key") String apiKey);

    @GET("/neo/rest/v1/neo/{id}")
    Call<NearEarthObject> nearEarthObject(@Path("id") String asteroidId, @Query("api_key") String apiKey);

    @GET("/neo/rest/v1/neo/browse")
    Call<NearEarthObjectBrowse> browse(@Query("api_key") String apiKey);
}
