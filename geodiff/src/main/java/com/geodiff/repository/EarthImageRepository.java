package com.geodiff.repository;

import com.geodiff.dto.Coordinate;
import me.rabrg.nasa.model.earth.EarthImage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;

public interface EarthImageRepository extends MongoRepository<EarthImage, String> {

    public EarthImage findByCoordinateAndDate(Coordinate coordinate, Date fecha);
}
