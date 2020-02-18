package com.geodiff.rest;

import com.geodiff.AppConfig;
import com.geodiff.WebProperties;
import com.geodiff.dto.Coordinate;
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
     * Initializa las colas temporales para los clientes.
     *
     *  @return                     Nombre de las Colas y parametros para conectarse a RabbitMQ.
     * */
    @GetMapping("/init-client-queues")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public HashMap<String, String> initClientQueues() throws IOException {
        return geoDiffService.initClientQueues();
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
    public void newMap(
            @RequestParam(name = "begin-date", required = true)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginDate,
            @RequestParam(name = "end-date", required = true)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(name = "resource_queue", required = true) String resourceClientQueue,
            @RequestParam(name = "results_queue", required = true) String resultClientQueue,
            @RequestBody ArrayList<Coordinate> coordinates) throws IOException, GeoException, ParseException {
        logger.info(" [+] Request arrived. Coordinates: " + coordinates );
        geoDiffService.createMapOptimized(coordinates, beginDate, endDate, resourceClientQueue, resultClientQueue);
    }
}
