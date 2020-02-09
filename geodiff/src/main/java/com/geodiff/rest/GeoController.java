package com.geodiff.rest;

import com.geodiff.dto.Coordinate;
import com.geodiff.dto.GeoAsset;
import com.geodiff.dto.GeoException;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    @RequestMapping("/")
    public String getIndex(Model model) {
        model.asMap();
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
    @PostMapping("/new-map")
    public String newMap(
            @RequestParam(name = "begin-date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginDate,
            @RequestParam(name = "end-date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestBody ArrayList<Coordinate> coordinates,
            Model model) throws GeoException
    {
        logger.info(" [+] Request arrived. ");
        HashMap<String, ArrayList<GeoAsset>> GeoAssets = geoDiffService.createMap(coordinates, beginDate, endDate);
        model.addAttribute("GeoAssets", GeoAssets);
        return "results";
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
    public HashMap<String, ArrayList<GeoAsset>> getAssets(
            @RequestParam(name = "begin-date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginDate,
            @RequestParam(name = "end-date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestBody ArrayList<Coordinate> coordinates) throws GeoException
    {
        return geoDiffService.createMap(coordinates, beginDate, endDate);
    }

    /**
     *  Get a Base64 encoded Image.
     *
     *  @param    date         Date
     *  @param    latitude     -
     *  @param    longitude    -
     *  @param    filter       Filter applied to the image defined in FilterOption.
     *  @return                GeoImage result.
     * */
    @GetMapping("/img")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public String getImage(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") Date date,
            @RequestParam(name = "lat", required = true) Double latitude,
            @RequestParam(name = "lon", required = true) Double longitude,
            @RequestParam(name = "filter", required = true) String filter ) throws GeoException
    {
        return geoDiffService.findGeoImage(latitude, longitude, date, filter).getEarthImage().getRawImage();
    }

    /**
     * Carga un nuevo mapa con parametros hardcodeados.
     *
     * */
    @GetMapping("/new-map-example")
    public String newMapExample(Model model) throws GeoException
    {
        ArrayList<Coordinate> a = new ArrayList<>();
        a.add(new Coordinate(1.5538599350392837, 100.72591781616211));
        a.add(new Coordinate(1.5538599350392837, 100.75063705444336));
        a.add(new Coordinate(1.5753096072435775, 100.75063705444336));
        a.add(new Coordinate(1.5753096072435775, 100.72591781616211));
        a.add(new Coordinate(1.5538599350392837, 100.72591781616211));
        Date d = null;
        try {
            d = new SimpleDateFormat("yyyy-MM-dd").parse("2014-02-04");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return this.newMap(d,null, a, model);
    }
}
