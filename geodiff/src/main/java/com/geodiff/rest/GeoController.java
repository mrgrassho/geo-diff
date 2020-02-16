package com.geodiff.rest;

import com.geodiff.AppConfig;
import com.geodiff.WebProperties;
import com.geodiff.dto.Coordinate;
import com.geodiff.dto.GeoException;
import com.geodiff.model.GeoImage;
import com.geodiff.service.GeoDiffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


@Controller
@RequestMapping("/geo")
@CrossOrigin
@Scope("session")
public class GeoController {

    private Logger logger = LoggerFactory.getLogger(GeoController.class);

    @Autowired
    GeoDiffService geoDiffService;

    @Autowired
    AppConfig appConfig;

    @Autowired
    WebProperties webProperties;

    @RequestMapping("/")
    public String getIndex(Model model) {
        model.asMap();
        model.addAttribute("url", webProperties.getUrl() + "/geo");
        model.addAttribute("dimension", appConfig.configData().DIMENSION);
        return "index";
    }

    /**
     * Carga un nuevo mapa.
     *
     *  @param    beginDate         FechaDesde >= fecha.
     *  @param    endDate           FechaHasta <= fecha.
     *  @param    coordinates       Lista de coordenadas que se quieren procesar.
     *  @return                     Lista de Imagenes.
     * */
    @PostMapping("/img-assets")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public void newMap(
            @RequestParam(name = "begin-date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginDate,
            @RequestParam(name = "end-date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestBody ArrayList<Coordinate> coordinates,
            @RequestBody String resourceClientQueue,
            @RequestBody String resultClientQueue) throws IOException, GeoException, ParseException {
        logger.info(" [+] Request arrived. Coordinates: " + coordinates );
        geoDiffService.createMapOptimized(coordinates, beginDate, endDate, resourceClientQueue, resultClientQueue);
    }

    /**
     * Carga un nuevo mapa.
     *
     *  @param    beginDate         FechaDesde >= fecha.
     *  @param    endDate           FechaHasta <= fecha.
     *  @param    coordinates       Lista de coordenadas que se quieren procesar.
     *  @return                     Lista de Imagenes.
     * */
    @PostMapping("/new-map")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public HashMap<String, String> createMap(
            @RequestParam(name = "begin-date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginDate,
            @RequestParam(name = "end-date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestBody ArrayList<Coordinate> coordinates) throws IOException {
        logger.info(" [+] Request arrived. Coordinates: " + coordinates );
        return geoDiffService.initClientRequest(coordinates, beginDate, endDate);
    }

    /**
     *  Get a Base64 encoded Image.
     *
     *  @param    date         Date
     *  @param    latitude     -
     *  @param    longitude    -
     *  @param    filter       Filter applied to the image defined in FilterOption.
     *  @return
     * */
    @GetMapping("/img")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public String getImage(
            @RequestParam(name = "date", required = true)
            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") Date date,
            @RequestParam(name = "lat", required = true) Double latitude,
            @RequestParam(name = "lon", required = true) Double longitude,
            @RequestParam(name = "filter", required = true) String filter ) throws GeoException
    {
        GeoImage gi = geoDiffService.findGeoImage(latitude, longitude, date, filter);
        if (gi != null) {
            return gi.getEarthImage().getRawImage();
        } else {
            return "";
        }
    }

    /**
     *  Get a Vector of Image with filter applied.
     *
     *  @param    id           Date
     *  @return                GeoImage result.
     * */
    @GetMapping("/vector")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public String getVector(
            @RequestParam(name = "date", required = true)
            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") Date date,
            @RequestParam(name = "lat", required = true) Double latitude,
            @RequestParam(name = "lon", required = true) Double longitude,
            @RequestParam(name = "filter", required = true) String filter )  throws GeoException
    {
        GeoImage gi = geoDiffService.findGeoImage(latitude, longitude, date, filter);
        if (gi != null) {
            return gi.getVectorImage();
        } else {
            return "";
        }
    }
}
