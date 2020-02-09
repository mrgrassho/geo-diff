package com.geodiff.service;


import com.geodiff.AppConfig;
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
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

@Service
@PropertySource("classpath:application.properties")
public class GeoDiffService {

    private static final boolean CLOUDSCORE = true;
    private static final Double DIMENSION = 0.025;
    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    private final DateFormat timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private Channel channel;
    private Gson gson;
    private Logger logger = LoggerFactory.getLogger(GeoDiffService.class);

    @Autowired
    private GeoImageRepository geoImageRepository;

    @Autowired
    private FilterOptionRepository filterOptionRepository;

    @Autowired
    public AppConfig appConfig;

    @Autowired
    public void initRabbitmq() throws URISyntaxException, KeyManagementException, TimeoutException, NoSuchAlgorithmException, IOException {
        this.initQueues();
        this.getResultFromQueue();
    }

    public HashMap<String, ArrayList<GeoAsset>> createMap(ArrayList<Coordinate> coordinates, Date beginDate, Date endDate) throws GeoException {
        NasaApi nasaApi = new NasaApi(appConfig.configData().API_KEY);
        ArrayList<EarthAssets> earthAssets = new ArrayList<>();
        HashMap<String, ArrayList<GeoAsset>> geoAssets =  new HashMap<>();
        try {
            String beginDateStr = (beginDate != null) ? df.format(beginDate): null;
            String endDateStr = (endDate != null) ? df.format(endDate): null;
            coordinates = transformBoxToList(coordinates);
            logger.info(" - Process Order Arrived - Quantity: " + coordinates.size() + ", BeginDate: " + beginDateStr + ", EndDate:  " + endDateStr);
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
                            if (e.getCloudScore() < appConfig.configData().CLOUDSCORE_MAX) {
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

    public void initQueues() throws IOException, TimeoutException, NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri( appConfig.configData().AMQP_URI);
        Connection connection = factory.newConnection();
        this.channel = connection.createChannel();
        this.channel.queueDeclare( appConfig.configData().TASK_QUEUE_NAME, true, false, false, null);
        this.channel.queueDeclare( appConfig.configData().RESULT_QUEUE_NAME, true, false, false, null);
        logger.info(" - Queues Declared: " + appConfig.configData().TASK_QUEUE_NAME + ", " + appConfig.configData().RESULT_QUEUE_NAME);
    }


    public void sendTaskToQueue(GeoImage gi) throws IOException {
        String m = gson.toJson(gi);
        channel.basicPublish("",  appConfig.configData().TASK_QUEUE_NAME,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    m.getBytes("UTF-8"));
    }

    public void getResultFromQueue() throws IOException {

        logger.info(" - Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            logger.info(" [*] New Image arrived. DATA: " + message.substring(0,60));
            GeoImage gi = gson.fromJson(message, GeoImage.class);
            geoImageRepository.save(gi);
        };
        channel.basicConsume( appConfig.configData().RESULT_QUEUE_NAME, true, deliverCallback, consumerTag -> { });
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
        logger.info(" BoxToList -> " + tmp_list);
        return tmp_list;
    }
}
