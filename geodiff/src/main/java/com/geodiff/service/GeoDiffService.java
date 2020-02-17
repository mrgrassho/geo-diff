package com.geodiff.service;


import com.geodiff.AppConfig;
import com.geodiff.dto.Coordinate;
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
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    private ConnectionFactory factory;

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";
    private static final String DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
    private static SecureRandom random = new SecureRandom();


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
    
    public void createMapOptimized(ArrayList<Coordinate> coordinates, Date beginDate, Date endDate, String resourceClientQueue, String resultClientQueue) throws GeoException, ParseException {
        NasaApi nasaApi = new NasaApi(appConfig.configData().API_KEY);
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
                            GeoImage gi = new GeoImage();;
                            if (null == (gi = this.findGeoImage(
                                    eas.getCoordinate().getLatitude(),
                                    eas.getCoordinate().getLongitude(),
                                    timestamp.parse(ea.getDate()),
                                    "RAW"))) {
                                EarthImage e = nasaApi.getEarthImage(eas.getCoordinate().getLatitude(), eas.getCoordinate().getLongitude(), appConfig.configData().DIMENSION, ea.getDate().split("T")[0], CLOUDSCORE);
                                // Si es una imagen muy nublada no la guardamos.
                                if ( appConfig.configData().CLOUDSCORE_MAX.compareTo(e.getCloudScore()) >= 0) {
                                    e.setCoordinate(eas.getCoordinate());
                                    e.setDim(appConfig.configData().DIMENSION);
                                    gi = new GeoImage();
                                    gi.setEarthImage(e);
                                    gi.setFilterOption(filterOptionRepository.findByName("RAW"));
                                    geoImageRepository.save(gi);
                                    this.sendGeoImageToQueue(gi, appConfig.configData().TASK_QUEUE_NAME, resourceClientQueue);
                                    this.sendGeoImageToQueue(gi, resourceClientQueue, "");
                                } else {
                                    logger.info(" Img is being ignored. The cloud_score is " +  e.getCloudScore());
                                }
                            } else {
                                this.sendGeoImageToQueue(gi, resourceClientQueue, "");
                                this.sendGeoImageToQueue(gi, resultClientQueue, "");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new GeoException(e.getMessage());
        }
    }

    public static String generateRandomString(int length) {
        if (length < 1) throw new IllegalArgumentException();

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            // 0-62 (exclusive), random returns 0-61
            int rndCharAt = random.nextInt(DATA_FOR_RANDOM_STRING.length());
            char rndChar = DATA_FOR_RANDOM_STRING.charAt(rndCharAt);
            sb.append(rndChar);
        }
        return sb.toString();

    }

    public HashMap<String, String> initClientQueues() throws IOException {
        HashMap<String, String> tmp =  new HashMap<>();
        // Temporary Resource Queue for given Client
        tmp.put("resource_queue", this.channel.queueDeclare(generateRandomString(60),true, false, true, null).getQueue());
        // Temporary Result Queue for given Client
        tmp.put("results_queue", this.channel.queueDeclare(generateRandomString(60),true, false, true, null).getQueue());
        tmp.put("user", factory.getUsername());
        tmp.put("pass", factory.getPassword());
        this.channel.queueBind(tmp.get("results_queue"), appConfig.configData().RESULT_XCHG_NAME, "");
        logger.info(" - Client Queue declared (results_queue): " + tmp.get("results_queue"));
        logger.info(" - Client Queue declared (resource_queue): " + tmp.get("resource_queue"));
        return tmp;
    }

    public HashMap<String, String> initClientRequest(ArrayList<Coordinate> coordinates, Date beginDate, Date endDate) throws IOException {
        HashMap<String, String> clientQueues = this.initClientQueues();
        Runnable runnable = () -> {
            try {
                this.createMapOptimized(coordinates, beginDate, endDate, clientQueues.get("resource_queue"), clientQueues.get("results_queue"));
            } catch (GeoException | ParseException e) {
                Thread t = Thread.currentThread();
                t.getUncaughtExceptionHandler().uncaughtException(t, e);
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
        return clientQueues;
    }

    public void initQueues() throws IOException, TimeoutException, NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        this.factory = new ConnectionFactory();
        this.factory.setUri( appConfig.configData().AMQP_URI);
        Connection connection = factory.newConnection();
        this.channel = connection.createChannel();
        this.channel.exchangeDeclare(appConfig.configData().RESULT_XCHG_NAME, "fanout");
        this.channel.queueDeclare( appConfig.configData().TASK_QUEUE_NAME, true, false, false, null);
        this.channel.queueDeclare( appConfig.configData().RESULT_QUEUE_NAME, true, false, false, null);
        this.channel.queueBind(appConfig.configData().RESULT_QUEUE_NAME, appConfig.configData().RESULT_XCHG_NAME, "");
        logger.info(" - Queues declared: " + appConfig.configData().TASK_QUEUE_NAME + ", " + appConfig.configData().RESULT_QUEUE_NAME);
        logger.info(" - Exchange declared: " + appConfig.configData().RESULT_XCHG_NAME + " bound to " + appConfig.configData().RESULT_QUEUE_NAME);
    }


    public void sendGeoImageToQueue(GeoImage gi, String queue, String replyMq) throws IOException {
        String m = gson.toJson(gi);
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().replyTo(replyMq).build();
        channel.basicPublish("",  queue,
                    props,
                    m.getBytes("UTF-8"));
        logger.info(" Image published to RabbitMQ Queue: " + m.substring(0, 120));
    }

    public void getResultFromQueue() throws IOException {

        logger.info(" - Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            GeoImage g = gson.fromJson(message, GeoImage.class);
            GeoImage gi = geoImageRepository.findByIdd(g.getId());
            if (gi != null) {
                gi.setVectorImage(g.getVectorImage());
                gi.getEarthImage().setRawImage(g.getEarthImage().getRawImage());
                geoImageRepository.save(gi);
                this.sendGeoImageToQueue(gi, delivery.getProperties().getReplyTo(), "");
                logger.info(" [*] New Image arrived. DATA: " + message.substring(0,60));
            } else {
                logger.error(" [!] Image NOT found in the DB. Ignoring result.");
            }

        };
        channel.basicConsume( appConfig.configData().RESULT_QUEUE_NAME, true, deliverCallback, consumerTag -> { });
    }

    public GeoImage findGeoImage(Double lat, Double lon, Date t, String nameFilter) throws GeoException {
        if ( lat == null  || lon == null || t == null || nameFilter == null) throw new GeoException("Null params found.") ;
        Coordinate coord = this.nearCoordinate(new Coordinate(lat, lon));
        logger.info(" [*] New Request for COORDS(lat, lon):" + coord + " - Date:"+ timestamp.format(t) + " - Name Filter:" + nameFilter);
        List<GeoImage> g = geoImageRepository.findByCoordinateAndDateAndFilter(coord.getLatitude(), coord.getLongitude(), timestamp.format(t));
        if (g.isEmpty()) {
            logger.info("Image Not Found.");
            return null;
        } else {
            logger.info("Image Coords (lat, lon)" + g.get(0).getEarthImage().getCoordinate());
            return g.get(0);
        }

    }

    public GeoImage findGeoImageByEarthImageId(String id) throws GeoException {
        if (id != null) {
            return geoImageRepository.findByEarthImageId(id).get(0);
        } else {
            throw new GeoException("Image Null");
        }
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
                tmp_list.add(new Coordinate(j, i));
            }
        }
        logger.info(" BoxToList -> " + tmp_list);
        return tmp_list;
    }
}
