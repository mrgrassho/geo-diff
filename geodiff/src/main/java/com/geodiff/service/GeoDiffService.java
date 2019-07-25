package com.geodiff.service;

import com.geodiff.dto.Coordinate;
import com.geodiff.dto.GeoException;
import com.geodiff.model.RawImage;
import me.rabrg.nasa.NasaApi;
import me.rabrg.nasa.model.earth.EarthAssets;
import me.rabrg.nasa.model.earth.EarthImage;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Service
public class GeoDiffService {

    private static final boolean CLOUDSCORE = true;
    private static final Double DIMENSION = 0.025;
    private static final String API_KEY = "6k9ilibCQcusmZl9RRczizWjFC7K0gkviEt2G4Qa";
    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    public ArrayList<RawImage> createMap(ArrayList<Coordinate> coordinates, Date beginDate, Date endDate) throws GeoException {
        NasaApi nasaApi = new NasaApi(API_KEY);
        try {
            ArrayList<RawImage> images = new ArrayList<>();
            HashMap<String, ArrayList<EarthImage>> res = new HashMap<>();
            String beginDateStr = (beginDate != null) ? df.format(beginDate): null;
            String endDateStr = (endDate != null) ? df.format(endDate): null;
            for (Coordinate coord : coordinates) {
                // TODO: Busco si se encuentra en la BD local

                // Busco en la API
                EarthAssets eas = nasaApi.getEarthAssets(coord.getLatitude(), coord.getLongitude(), beginDateStr, endDateStr);
                ArrayList<EarthImage> ei = new ArrayList<>();
                if (eas.getCount() > 0) {
                    for (EarthAssets.EarthAsset ea: eas.getResults()) {
                        try {
                            EarthImage e = nasaApi.getEarthImage(coord.getLatitude(), coord.getLongitude(), DIMENSION, ea.getDate().split("T")[0], CLOUDSCORE);
                            ei.add(e);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                res.put(coord.toString(), ei);
            }
            // Cargar las imagenes

            // TODO: Guardar las imagenes en base
            System.err.println(res);
            // Cliente RabbitMQ envia trabajos a la WORK QUEUE
            return images;

        } catch (IOException e) {
            throw new GeoException(e.getMessage());
        }
    }

    //public ArrayList<RawImage> loadImages(ei);
}
