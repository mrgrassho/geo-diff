package com.geodiff.repository;

import com.geodiff.dto.Coordinate;
import com.geodiff.model.GeoImage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface GeoImageRepository extends MongoRepository<GeoImage, String> {

    @Query("{ 'earthImage': { 'coordinate' : ?0, 'date' : { $regex: ?1 } }, " +
            " 'filterOption': { name : ?2 }}")
    public GeoImage findByCoordinateDateAndFilter(Coordinate coordinate, String date, String nameFilter);

    @Query("{ 'earthImage': { 'coordinate' : ?0, 'date' : { $lte: { $regex: ?1 }} }, " +
            " 'filterOption': { name : ?2 }}")
    public GeoImage findByCoordinateLTEDateAndFilter(Coordinate coordinate, String date, String nameFilter);

}
