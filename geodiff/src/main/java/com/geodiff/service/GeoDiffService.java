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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
    private final String dateTimePattern =  "yyyy-MM-dd";
    private final DateTimeFormatter dtf = DateTimeFormat.forPattern(dateTimePattern);
    private final DateFormat df = new SimpleDateFormat(dateTimePattern);
    private final DateFormat timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private Channel channel;
    private Gson gson = new Gson();
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
                if (eas.getCount() > 0) {
                    eas.setCoordinate(coord);
                    for (int i = 0; i < eas.getCount(); i++) {
                        EarthAssets.EarthAsset ea = eas.getResults().get(i);
                        try {
                            if (null == geoImageRepository.findByIdd(eas.getResults().get(i).getId())) {
                                EarthImage e = nasaApi.getEarthImage(eas.getCoordinate().getLatitude(), eas.getCoordinate().getLongitude(), appConfig.configData().DIMENSION, ea.getDate().split("T")[0], CLOUDSCORE);
                                // Si es una imagen muy nublada no la guardamos.
                                if (e.getCloudScore() < appConfig.configData().CLOUDSCORE_MAX) {
                                    e.setCoordinate(eas.getCoordinate());
                                    e.setDim(appConfig.configData().DIMENSION);
                                    GeoImage gi = new GeoImage();
                                    gi.setEarthImage(e);
                                    gi.setFilterOption(filterOptionRepository.findByName("RAW"));
                                    geoImageRepository.save(gi);
                                    this.sendTaskToQueue(gi);
                                    earthAssets.add(eas);
                                } else {
                                    logger.info(" Img is being ignored. The cloud_score is " +  e.getCloudScore());
                                }
                            } else {
                                earthAssets.add(eas);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

//            for (EarthAssets eas: earthAssets) {
//                for (int i = 0; i < eas.getCount(); i++) {
//                    EarthAssets.EarthAsset ea = eas.getResults().get(i);
//                    try {
//                        GeoImage gi;
//                        if (null == (gi = geoImageRepository.findByCoordinateDateAndFilter(eas.getCoordinate().getLatitude(), eas.getCoordinate().getLongitude(), regexBeginWith(ea.getDate().split("T")[0]), "RAW"))) {
//                            EarthImage e = nasaApi.getEarthImage(eas.getCoordinate().getLatitude(), eas.getCoordinate().getLongitude(), appConfig.configData().DIMENSION, ea.getDate().split("T")[0], CLOUDSCORE);
//                            // Si es una imagen muy nublada no la guardamos.
//                            if (e.getCloudScore() < appConfig.configData().CLOUDSCORE_MAX) {
//                                e.setCoordinate(eas.getCoordinate());
//                                e.setDim(appConfig.configData().DIMENSION);
//                                gi = new GeoImage();
//                                gi.setEarthImage(e);
//                                gi.setFilterOption(filterOptionRepository.findByName("RAW"));
//                                geoImageRepository.save(gi);
//                                this.sendTaskToQueue(gi);
//                                earthAssets.add(eas);
//                            }
//                        } else {
//                            earthAssets.add(eas);
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }


            // SE ASUME QUE TODOS LOS earthAssets tienen la misma LONGITUD
            // REVISAR ESTO PARA QUE CARGUE UNA IMAGEN VACIA SI NO
            // se tiene el resultado
            if (!earthAssets.isEmpty()){
                for (int i = 0; i < earthAssets.size(); i++) {
                    String groupDate = "";
                    for (int j = 0; j <  earthAssets.get(i).getCount(); j++) {
                        ArrayList<GeoAsset> ga;
                        String ts = earthAssets.get(i).getResults().get(j).getDate();
                        String id = earthAssets.get(i).getResults().get(j).getId();
                        String date = earthAssets.get(i).getResults().get(j).getDate().split("T")[0];
                        groupDate = (Integer.valueOf(dtf.parseDateTime(date).toString("dd")) < 16) ? dtf.parseDateTime(date).withDayOfMonth(1).toString(dateTimePattern) : dtf.parseDateTime(date).withDayOfMonth(16).toString(dateTimePattern);
                        if (null == (ga = geoAssets.get(groupDate))) {
                            ga = new ArrayList<>();
                        }
                        Coordinate coord = earthAssets.get(i).getCoordinate();
                        ga.add(new GeoAsset(id, ts, coord));
                        geoAssets.put(groupDate, ga);
                    }
                    logger.info(" [*] New Coordinates Group. Key: " + groupDate + ", Qty: " + geoAssets.get(groupDate).size());
                }
            }

            return geoAssets;

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
        logger.info(" Image published to RabbitMQ Queue: " + m.substring(0, 120));
    }

    public void getResultFromQueue() throws IOException {

        logger.info(" - Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            GeoImage g = gson.fromJson(message, GeoImage.class);
            GeoImage gi = geoImageRepository.findByIdd(g.getId());
            gi.setVectorImage(g.getVectorImage());
            geoImageRepository.save(gi);
            logger.info(" [*] New Image arrived. DATA: " + message.substring(0,60));
        };
        channel.basicConsume( appConfig.configData().RESULT_QUEUE_NAME, true, deliverCallback, consumerTag -> { });
    }

    public GeoImage findGeoImage(Double lat, Double lon, Date t, String nameFilter) throws GeoException {
        if ( lat == null  || lon == null || t == null || nameFilter == null) throw new GeoException("Null params found.") ;
        Coordinate coord = this.nearCoordinate(new Coordinate(lat, lon));
        logger.info(" [*] New Request for COORDS(lat, lon):" + coord + " - Date:"+ timestamp.format(t) + " - Name Filter:" + nameFilter);
        GeoImage g = geoImageRepository.findByCoordinateAndDateAndFilter(coord.getLatitude(), coord.getLongitude(), timestamp.format(t), nameFilter);
        if (g == null){
            throw new GeoException("Image Null");
        }
        return g;
    }

    public GeoImage findGeoImageByEarthImageId(String id) throws GeoException {
        if (id != null) {
            return geoImageRepository.findByEarthImageId(id);
        } else {
            throw new GeoException("Image Null");
        }
    }

    public String regexBeginWith(String str) {
        return "^" + str;
    }

    public double getNearPoint(double x, double y){
        if ((x * 1000) % (y * 1000) != 0) {
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
        c.setLatitude( this.getNearPoint(coord.getLatitude(), appConfig.configData().DIMENSION));
        c.setLongitude( this.getNearPoint(coord.getLongitude(), appConfig.configData().DIMENSION));
        return c;
    }

    public ArrayList<Coordinate> transformBoxToList(ArrayList<Coordinate> coords){
        logger.info(" Initial coords -> " + coords);
        for (int i = 0; i < coords.size(); i++) {
            coords.set(i, nearCoordinate(coords.get(i)));
        }
        logger.info(" Centered coords -> " + coords);
        ArrayList<Coordinate> tmp_list = new ArrayList<Coordinate>();
        for (double i = coords.get(0).getLongitude(); i <= coords.get(1).getLongitude(); i = i + appConfig.configData().DIMENSION){
            for (double j = coords.get(0).getLatitude(); j <= coords.get(3).getLatitude(); j = j + appConfig.configData().DIMENSION){
                tmp_list.add(new Coordinate(i, j));
            }
        }
        logger.info(" BoxToList -> " + tmp_list);
        return tmp_list;
    }
}
