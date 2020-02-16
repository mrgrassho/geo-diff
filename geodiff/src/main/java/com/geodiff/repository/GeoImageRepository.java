package com.geodiff.repository;

import com.geodiff.model.GeoImage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface GeoImageRepository extends MongoRepository<GeoImage, String> {

    @Query("{ " +
            "  'earthImage.coordinate.latitude': ?0, " +
            "  'earthImage.coordinate.longitude': ?1, " +
            "  'earthImage.date': ?2 " +
            " }")
    public List<GeoImage> findByCoordinateAndDateAndFilter(double latitude, double longitude, String date);

    @Query("{ '_id': ?0}")
    public GeoImage findByIdd(String id);

    @Query("{ 'earthImage._id': ?0}")
    public List<GeoImage> findByEarthImageId(String id);

}
