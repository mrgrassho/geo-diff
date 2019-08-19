package com.geodiff.rest;

import com.geodiff.dto.Coordinate;
import com.geodiff.dto.GeoException;
import com.geodiff.model.GeoImage;
import com.geodiff.service.GeoDiffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


@Controller
@RequestMapping("/geo")
@CrossOrigin
@Scope("session")
public class GeoController {

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
        ArrayList<GeoImage> eis = geoDiffService.createMap(coordinates, beginDate, endDate);
        model.addAttribute("earthImages", eis);
        return "results";
    }

    @GetMapping("/img")
    public GeoImage getImage(
            @RequestParam(name = "near-date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date nearDate,
            @RequestParam(name = "lat", required = true) Double latitude,
            @RequestParam(name = "lon", required = true) Double longitude,
            @RequestParam(name = "filter", required = true) String filter ) throws GeoException

    {
        return geoDiffService.findGeoImageLessEqualDate(latitude, longitude, nearDate, filter);
    }

    /**
     * Carga un nuevo mapa con parametros hardcodeados.
     *
     * */
    @GetMapping("/new-map-example")
    public String newMapExample(Model model) throws GeoException
    {
        ArrayList<Coordinate> a = new ArrayList<>();
        a.add(new Coordinate(-3.756, -62.153));
        a.add(new Coordinate(-3.756, -62.163));
        Date d = null;
        try {
            d = new SimpleDateFormat("yyyy-MM-dd").parse("2017-01-01");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return this.newMap(d,null, a, model);
    }

}
