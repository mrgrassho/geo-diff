package com.geodiff.repository;

import com.geodiff.dto.Coordinate;
import com.geodiff.model.RawImage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;

public interface EarthImageGeoRepository extends MongoRepository<RawImage, String> {

    public RawImage findByCoordinateAndDate(Coordinate coordinate, Date fecha);
}
