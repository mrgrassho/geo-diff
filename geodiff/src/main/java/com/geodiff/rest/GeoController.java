package com.geodiff.rest;

import com.geodiff.dto.Coordinate;
import com.geodiff.dto.GeoException;
import com.geodiff.service.GeoDiffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
     * Crea un nuevo movimiento.
     *
     *  @param    coordinates       Lista de coordenadas que se quieren procesar.
     *  @param    beginDate         FechaDesde >= fecha.
     *  @param    endDate           FechaHasta <= fecha.
     *  @return                     Lista de Imagenes.
     * */
    @PostMapping("/new-map")
    public String newMap(
            @RequestParam(name = "begin-date",required = false)
            @DateTimeFormat( pattern = "yyyy-MM-dd") Date beginDate,
            @RequestParam(name = "end-date",required = false)
            @DateTimeFormat( pattern = "yyyy-MM-dd") Date endDate,
            @RequestBody ArrayList<Coordinate> coordinates,
            Model model) throws GeoException
    {
        geoDiffService.createMap(coordinates, beginDate, endDate);
        return "results";
    }

}
