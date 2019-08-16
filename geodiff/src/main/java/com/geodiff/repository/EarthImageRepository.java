package com.geodiff.repository;

import com.geodiff.dto.Coordinate;
import com.nasa.model.earth.EarthImage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface EarthImageRepository extends MongoRepository<EarthImage, String> {

    @Query("{ 'coordinate' : ?0, 'date' : { $regex: ?1 } }")
    public EarthImage findByCoordinateAndDate(Coordinate coordinate, String date);
}
