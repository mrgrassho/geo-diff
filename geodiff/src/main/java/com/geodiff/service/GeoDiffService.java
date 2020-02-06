package com.geodiff.service;


import com.geodiff.dto.Coordinate;
import com.geodiff.dto.GeoAsset;
import com.geodiff.dto.GeoException;
import com.geodiff.model.GeoImage;
import com.geodiff.repository.FilterOptionRepository;
import com.geodiff.repository.GeoImageRepository;
import com.google.gson.Gson;
import com.nasa.NasaApi;
import com.nasa.model.earth.EarthAssets;
import com.nasa.model.earth.EarthImage;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Service
public class GeoDiffService {

    // TODO: Replace constants with configuration file.
    private static final boolean CLOUDSCORE = true;
    private static final Double CLOUDSCORE_MAX = 0.2;
    private static final Double DIMENSION = 0.025;
    private static final String API_KEY = "6k9ilibCQcusmZl9RRczizWjFC7K0gkviEt2G4Qa";
    private static final String TASK_QUEUE_NAME = "TASK_QUEUE";
    private static final String RESULT_QUEUE_NAME = "RESULT_QUEUE";

    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    private final DateFormat timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private Channel channel;
    private Gson gson;

    @Autowired
    private GeoImageRepository geoImageRepository;

    @Autowired
    private FilterOptionRepository filterOptionRepository;

    public HashMap<String, ArrayList<GeoAsset>> createMap(ArrayList<Coordinate> coordinates, Date beginDate, Date endDate) throws GeoException {
        NasaApi nasaApi = new NasaApi(API_KEY);
        ArrayList<EarthAssets> earthAssets = new ArrayList<>();
        HashMap<String, ArrayList<GeoAsset>> geoAssets =  new HashMap<>();
        try {
            String beginDateStr = (beginDate != null) ? df.format(beginDate): null;
            String endDateStr = (endDate != null) ? df.format(endDate): null;
            coordinates = transformBoxToList(coordinates);
            for (Coordinate coord : coordinates) {
                EarthAssets eas = nasaApi.getEarthAssets(coord.getLatitude(), coord.getLongitude(), beginDateStr, endDateStr);
                eas.setCoordinate(coord);
                earthAssets.add(eas);
            }

            for (EarthAssets eas: earthAssets) {
                if (eas.getCount() > 0) {
                    EarthAssets.EarthAsset ea = eas.getResults().get(0);
                    try {
                        GeoImage gi;
                        if (null == (gi = geoImageRepository.findByCoordinateDateAndFilter(eas.getCoordinate().getLatitude(), eas.getCoordinate().getLongitude(), regexBeginWith(ea.getDate().split("T")[0]), "RAW"))) {
                            EarthImage e = nasaApi.getEarthImage(eas.getCoordinate().getLatitude(), eas.getCoordinate().getLongitude(), DIMENSION, ea.getDate().split("T")[0], CLOUDSCORE);
                            // Si es una imagen muy nublada no la guardamos.
                            if (e.getCloudScore() < CLOUDSCORE_MAX) {
                                e.setCoordinate(eas.getCoordinate());
                                e.setDim(DIMENSION);
                                gi = new GeoImage();
                                gi.setEarthImage(e);
                                gi.setFilterOption(filterOptionRepository.findByName("RAW"));
                                geoImageRepository.save(gi);
                                this.sendTaskToQueue(gi);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    eas.removeResult(ea);
                }
            }


            // SE ASUME QUE TODOS LOS earthAssets tienen la misma LONGITUD
            // REVISAR ESTO PARA QUE CARGUE UNA IMAGEN VACIA SI NO
            // se tiene el resultado
            if (!earthAssets.isEmpty()){
                for (int i = 0; i < earthAssets.get(0).getCount(); i++) {
                    for (int j = 0; j < earthAssets.size(); j++) {
                        ArrayList<GeoAsset> ga;
                        if (null == (ga = geoAssets.get(String.valueOf(i)))) {
                            ga = new ArrayList<>();
                        }
                        String date = earthAssets.get(j).getResults().get(i).getDate();
                        Coordinate coord = earthAssets.get(j).getCoordinate();
                        ga.add(new GeoAsset(date, coord));
                        geoAssets.put(String.valueOf(i), ga);
                    }
                }
            }

            return geoAssets;

            // Cliente RabbitMQ envia trabajos a la TASK QUEUE

        } catch (IOException e) {
            throw new GeoException(e.getMessage());
        }
    }

    public void initQueue() throws IOException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        this.channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
    }


    public void sendTaskToQueue(GeoImage gi) throws IOException {
        String m = gson.toJson(gi);
        channel.basicPublish("", TASK_QUEUE_NAME,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    m.getBytes("UTF-8"));
    }

    public EarthImage getResultFromQueue() throws IOException {
        GetResponse res = channel.basicGet(RESULT_QUEUE_NAME, true);
        return gson.fromJson(res.getBody().toString(), EarthImage.class);
    }

    public GeoImage findGeoImage(Double lat, Double lon, Date t, String nameFilter) throws GeoException {
        if ( lat == null  || lon == null || t == null || nameFilter == null) throw new GeoException("Null params found.") ;
        Coordinate coord = this.nearCoordinate(new Coordinate(lat, lon));
        return geoImageRepository.findByCoordinateAndDateAndFilter(coord.getLatitude(), coord.getLongitude(), timestamp.format(t), nameFilter);
    }

    public String regexBeginWith(String str) {
        return "^" + str;
    }

    public double getNearPoint(double x, double y){
        if ((x * 1000) % (y * 1000) == 0) {
            return (double) ((int) ((x * 1000) / (y * 1000)) * (y * 1000) ) / 1000;
        } else {
            return (double) ((int) ((x * 1000) / (y * 1000)) * (y * 1000) + (y * 1000)) / 1000;
        }
    }

    /**
     *  Get the coordinate that is saved in the DB which is the near coordinate
     *  of the requested one.
     *
     *  @param    coord        Coordinate.
     *  @return                Coordinate.
     * */
    public Coordinate nearCoordinate(Coordinate coord){
        Coordinate c =  new Coordinate();
        c.setLatitude( this.getNearPoint(coord.getLatitude(), DIMENSION));
        c.setLongitude( this.getNearPoint(coord.getLongitude(), DIMENSION));
        return c;
    }

    public ArrayList<Coordinate> transformBoxToList(ArrayList<Coordinate> coords){
        for (int i = 0; i < coords.size(); i++) {
            coords.set(i, nearCoordinate(coords.get(i)));
        }

        ArrayList<Coordinate> tmp_list = new ArrayList<Coordinate>();
        for (double i = coords.get(0).getLatitude(); i < coords.get(1).getLatitude(); i = i + DIMENSION){
            for (double j = coords.get(0).getLongitude(); j < coords.get(3).getLongitude(); j = j + DIMENSION){
                tmp_list.add(new Coordinate(i, j));
            }
        }
        System.out.println(tmp_list);
        return tmp_list;
    }
}
