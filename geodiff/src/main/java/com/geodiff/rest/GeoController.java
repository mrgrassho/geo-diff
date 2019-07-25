package com.geodiff.service;

import com.geodiff.dto.Coordinate;
import org.springframework.context.annotation.Scope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;


@RestController
@RequestMapping("/geo")
@CrossOrigin
@Scope("session")
public class GeoController {

    private GeoController(){}

    /**
     * Crea un nuevo movimiento.
     *
     * @param    coordinates       Lista de coordenadas que se quieren procesar.
     * @param    coordinates       Lista de coordenadas que se quieren procesar.
     **/
    @PostMapping("/new-map")
    public void crearMapa(
            @RequestParam(name = "fecha-desde",required = true)
            @DateTimeFormat( pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(name = "fecha-hasta",required = true)
            @DateTimeFormat( pattern = "yyyy-MM-dd") Date fechaHasta,
            @RequestBody ArrayList<Coordinate> coordinates)
    {

    }

}
