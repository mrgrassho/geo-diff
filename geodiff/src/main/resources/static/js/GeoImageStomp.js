var GeoImage = GeoImage || {},
    document = document || {},
    window   = window   || {};

var Map = ol.Map;   //import Map from 'ol/Map.js';
var View = ol.View; //import View from 'ol/View.js';
var Draw = ol.interaction.Draw; //import Draw from 'ol/interaction/Draw.js';
var TileLayer = ol.layer.Tile; //import {Tile as TileLayer, Vector as VectorLayer} from 'ol/layer.js';
var ImageLayer = ol.layer.Image;
var VectorLayer = ol.layer.Vector; //import {Tile as TileLayer, Vector as VectorLayer} from 'ol/layer.js';
var OSM = ol.source.OSM; //import {OSM, Vector as VectorSource} from 'ol/source.js';
var VectorSource = ol.source.Vector; //import {OSM, Vector as VectorSource} from 'ol/source.js';
var Static = ol.source.ImageStatic;
var GeoJSON = ol.format.GeoJSON;
var proj = ol.proj;
var Ajax = Ajax;
var Fill = ol.style.Fill;
var Stroke = ol.style.Stroke;
var Style = ol.style.Style;
var Color = ol.color;
var Text = ol.style.Text;
const monthNames = ["January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"];

// Inician el cliente RabbitMQ, establecen las colas y/o mensajes de error. ---------------------------------------------
GeoImage.initClient = function () {
  // LLAMADA AJAX PARA INICIALIZAR QUEUES
  let url = GeoImage.baseURL + "/init-client-queues";
  Ajax.request( "GET", url, {}, function (xhr) {
  // LLEGA EL RECURSO Y SE AGREGA LA CAPA.
    var obj = JSON.parse(xhr);
    if (xhr === "{}") {
        console.log("[!] Request Object are NOT available. Request: " + url);
    } else {
        GeoImage.resourceClientQueue = obj['resource_queue']
        GeoImage.resultClientQueue = obj['results_queue']
        GeoImage.rabbitMQ = {}
        GeoImage.rabbitMQ.u = obj['user']
        GeoImage.rabbitMQ.p = obj['pass']
        GeoImage.initRabbitMQ()
    }
  });
}

GeoImage.initRabbitMQ =  function() {
  console.log('> Running function initRabbitMQ');
  GeoImage.client = Stomp.client('ws://' + window.location.hostname + ':15674/ws');
  GeoImage.client.connect(GeoImage.rabbitMQ.u, GeoImage.rabbitMQ.p , GeoImage.onConnectRabbitMQ, GeoImage.onErrorRabbitMQ, '/');
}

GeoImage.onConnectRabbitMQ = function(x) {
  GeoImage.client.subscribe(GeoImage.resourceClientQueue, GeoImage.onResourceMessage, {'auto-delete': true});
  GeoImage.client.subscribe(GeoImage.resultClientQueue,  GeoImage.onResultMessage, {'auto-delete': true});
}

GeoImage.onErrorRabbitMQ =  function(frame) {
  alert("[!] RabbitMQ server is NOT available.");
  console.log(' [!] Error: ', frame);
  let map = document.getElementById('map');
  map.innerHTML = '';
  GeoImage.resetSlider();
  GeoImage.init();
};
//-------------------------------------------------------------------------------------------------------------------------
// Se pide una imagen a partir de lo seleccionado en el mapa---------------------------------------------------------------
GeoImage.requestNewMap = function (coords) {
  // LLAMADA AJAX
  let beginDate = document.getElementById('begin-date').value;
  let endDate = document.getElementById('end-date').value;
  let url = GeoImage.buildNewMapURL(beginDate, endDate);
  Ajax.request("POST", url, coords, function (xhr) {
    console.log(xhr);
  });
}

GeoImage.buildNewMapURL = function (beginDate, endDate) {
  beginDate = beginDate == null ? '': beginDate;
  endDate = endDate == null ? '': endDate;
  return GeoImage.baseURL + "/new-map?" +
               "begin-date=" + beginDate + "&" +
               "end-date=" + endDate + "&"+
               "resource_queue=" + GeoImage.resourceClientQueue + "&" +
               "results_queue=" + GeoImage.resultClientQueue + "&";
}
//------------------------------------------------------------------------------------------------------------------------------------
// Establece grupo (fecha) para la imagen/resource recibido
GeoImage.getDateGroup =  function(date_str) {
  let day = (parseInt(date_str.split('T')[0].slice(-2)) < 16) ? "01" : "16";
  return date_str.split('T')[0].slice(0, 8) + day
}

//Establece un grupo de fecha y agrega el a la slider de resultados el RESOURCE recibido.
GeoImage.onResourceMessage = function(d) {
  let geoImage = JSON.parse(d.body);
  let group = GeoImage.getDateGroup(geoImage["earthImage"]["date"]);
  console.log("[+] Resource available. Date "+ group);
  if (GeoImage.resources[group] == null) {
    GeoImage.resources[group] = [];
    GeoImage.color[group] = GeoImage.getRandomColor();
  }
  if (geoImage["filteredImages"] != null) {
      for (let p of geoImage["filteredImages"]) {
        geoImage["geoId_" + p["filterName"]]  = GeoImage.getHashCode(geoImage, p["filterName"]);
      }
  }
  geoImage["geoIdRaw"]  = GeoImage.getHashCode(geoImage, "RAW");
  GeoImage.resources[group].push(geoImage);
  console.log("[+] Added group to resources[] > "+ group);
  GeoImage.initSlider();
  let groupDate = new Date(group);
  //Si el usuario todavia no hizo click en ningun filtro, sigo cargando.
  if (!GeoImage.slider_active){
      GeoImage.addYearToSlider(groupDate.getFullYear());
  }
  //GeoImage.addDateToSlider(group);
}

//Establece un grupo de fecha y prepara la imagen del RESULT recibido.
GeoImage.onResultMessage = function(d) {
  let geoImage = JSON.parse(d.body);
  let group = GeoImage.getDateGroup(geoImage["earthImage"]["date"]);
  console.log(" [+] Result image available. Date:" + group);
  if (Object.entries(GeoImage.Layers).length === 0 && GeoImage.Layers.constructor === Object) {
    GeoImage.selectedGroup = group;
  }
  GeoImage.resultVector(geoImage);
  GeoImage.resultImage(geoImage);
  if (group == GeoImage.selectedGroup) {
    //GeoImage.loadMapCentralized(group)
  }
}

GeoImage.resultVector = function(item) {
  let offset = item["earthImage"]["dim"] / 2;
  let minLatLon = [item["earthImage"]["coordinate"]['longitude']-offset, item["earthImage"]["coordinate"]['latitude']-offset];
  let maxLatLon = [item["earthImage"]["coordinate"]['longitude']+offset, item["earthImage"]["coordinate"]['latitude']+offset];
  if (item["filteredImages"] != null) {
      for (let filteredImage of item["filteredImages"]) {
        let geoIdRaw = GeoImage.getHashCode(item, filteredImage["filterName"]);
        GeoImage.Layers[geoIdRaw] = new ImageLayer({
            source: new Static({
              imageLoadFunction : function(image){ image.getImage().src = filteredImage["vectorImage"]; },
              crossOrigin: '',
              imageExtent: [minLatLon[0], minLatLon[1], maxLatLon[0], maxLatLon[1]]
            })
          });
        GeoImage.Map.addLayer(GeoImage.Layers[geoIdRaw]);
        GeoImage.Layers[geoIdRaw].setVisible(false);
      }
  }
}

GeoImage.resultImage = function(item) {
  let offset = item["earthImage"]["dim"] / 2;
  let minLatLon = [item["earthImage"]["coordinate"]['longitude']-offset, item["earthImage"]["coordinate"]['latitude']-offset];
  let maxLatLon = [item["earthImage"]["coordinate"]['longitude']+offset, item["earthImage"]["coordinate"]['latitude']+offset];
  let geoIdRaw = GeoImage.getHashCode(item, "RAW");
  GeoImage.Layers[geoIdRaw] = new ImageLayer({
    source: new Static({
      imageLoadFunction : function(image){ image.getImage().src = item["earthImage"]["rawImage"]; },
      crossOrigin: '',
      imageExtent: [minLatLon[0], minLatLon[1], maxLatLon[0], maxLatLon[1]]
    })
  });
  GeoImage.Map.addLayer(GeoImage.Layers[geoIdRaw]);
  GeoImage.Layers[geoIdRaw].setVisible(false);
}

GeoImage.getRandomColor = function () {
  console.log('[+] Running function getRandomColor');
  var letters = '0123456789ABCDEF';
  var color = '#';
  for (var i = 0; i < 6; i++) {
    color += letters[Math.floor(Math.random() * 16)];
  }
  return Color.asArray(color);
}


GeoImage.getHashCode = function(item, args){
  let genrateHashCode = function(s){
    return s.split("").reduce(function(a,b){a=((a<<5)-a)+b.charCodeAt(0);return a&a},0);
  }
  let str = JSON.stringify(item["earthImage"]["coordinate"]) + item["earthImage"]["date"] + args;
  return genrateHashCode(str);
}

//Crea la barra para filtrar las imagenes, y el path que indica que filtros se aplico
GeoImage.initSlider = function() {
  if (!GeoImage.initSlider_status) {
    let list = document.getElementById('result-groups-100');
    let div = document.getElementById('geometry-type-100');
    div.setAttribute('style', 'display: none');
    let ol = document.createElement('div');
    ol.setAttribute('id', 'results-dates-100');
    //inserto la barra de path : indica que filtros se selecciona
    let path = document.createElement('div');
    path.classList.add('path');
    path.innerText = "Path: ";
        //agrega el año en el path
        let pathYear = document.createElement('a');
        pathYear.classList.add('year');
        pathYear.innerText = ' > Year';
        pathYear.setAttribute('current-year',0);
        pathYear.onclick = function(){//Para la accion de volver el filtro hacia atras.
            GeoImage.cleanSlider();
            GeoImage.loadYearsOnSlider();
            GeoImage.resetSliderPath(true,true,true);
        };
        path.appendChild(pathYear);
        //agrega el mes en el path
        let pathMonth = document.createElement('a');
        pathMonth.classList.add('month');
        pathMonth.innerText = ' > Month';
        pathMonth.setAttribute('current-month',0);
        pathMonth.onclick = function(){ //Accion de volver el filtro hacia atras.
            let c_y = document.querySelector('.path .year').getAttribute('current-year');
            if (c_y!=0){
                GeoImage.addMonthsToSlider(c_y);
                GeoImage.resetSliderPath(false,true,true);
            }
        }
        path.appendChild(pathMonth);
        //agrega el Dia en el path
        let pathDay = document.createElement('a');
        pathDay.classList.add('day');
        pathDay.innerText = ' > Day';
        pathDay.setAttribute('current-day',0);
        path.appendChild(pathDay);
    ol.appendChild(path);
    list.appendChild(ol);
    GeoImage.initSlider_status = true;
  }
}

//resetea los valores del path segun los parametros indicados.
GeoImage.resetSliderPath = function(reset_year,reset_month,reset_day){
  if (reset_year==true){
      let p_y = document.querySelector('.path .year');
      p_y.setAttribute('current-year',0);
      p_y.innerText = ' > Year';
  }
  if (reset_month==true){
      let p_m = document.querySelector('.path .month');
      p_m.setAttribute('current-month',0);
      p_m.innerText = ' > Month';
  }
  if (reset_day==true){
      let p_d = document.querySelector('.path .day');
      p_d.setAttribute('current-day',0);
      p_d.innerText = ' > Day'
  }
}

//Quita todos los elementos del slider (salvo la barra de path)
GeoImage.cleanSlider = function(){
      let resultsElements = document.querySelectorAll('#results-dates-100 div');
      resultsElements.forEach(function(element){
          if (!element.classList.contains('path')){
              resultsArea = document.getElementById('results-dates-100');
              resultsArea.removeChild(element);
          }
      });
}

//Agrega los años al slider
GeoImage.loadYearsOnSlider = function(){
    let dates = Object.keys(GeoImage.resources);
    for (i in dates){
        let dateX = new Date(dates[i]);
        let yearX = dateX.getFullYear();
        GeoImage.addYearToSlider(yearX);
    }
}
//agrega el año indicado al slider
GeoImage.addYearToSlider = function(year){
    let yearExist = document.querySelector('#results-dates-100 .year a[year="'+year+'"]');
    if (yearExist == null){
        let list = document.getElementById('results-dates-100');
        let li = document.createElement('div');
        li.classList.add('year');
        let a = document.createElement('a');
        a.setAttribute('year', year);
        a.innerText = year;
        a.classList.add('pop-up');
        li.appendChild(a);
        list.appendChild(li);
        a.setAttribute('onclick', 'GeoImage.addMonthsToSlider(\"'+year+'\");');
        console.log("[+] Year "+year+" added on the slider");
    }
    //else {console.log("[-] Year "+year+" already exists in the slider");}
}

//Agrega los meses cuando se hace click en un año del slider
GeoImage.addMonthsToSlider = function(year){
    GeoImage.slider_active = true;
    //actualizo el path con el año seleccionado
    let pathYear = document.querySelector('.path .year');
    pathYear.innerText = " > " + year;
    pathYear.setAttribute('current-year',year);
    //elimino elementos del slider hasta ese momento
    GeoImage.cleanSlider();
    //recorro las fechas e inserto en el slider
    let dates = Object.keys(GeoImage.resources); // ((las keys de este array son las fechas))
    for (i in dates){
        let dateX = new Date(dates[i]);

        let yearX = dateX.getFullYear();
        let monthX = dateX.getMonth()+1; // libreria moment() cuenta los meses de 0 a 11
        let monthExist = document.querySelector('#results-dates-100 .month a[month="'+monthX+'"][year="'+yearX+'"]');
        if (monthExist == null){
            let list = document.getElementById('results-dates-100');
            let li = document.createElement('div');
            li.classList.add('month');
            let a = document.createElement('a');
            a.setAttribute('year', yearX);
            a.setAttribute('month', monthX);
            let monthString = monthNames[dateX.getMonth()];
            a.innerText = monthString.substring(0,3);
            a.classList.add('pop-up');
            li.appendChild(a);
            list.appendChild(li);
            a.setAttribute('onclick', 'GeoImage.addDatesToSlider("'+yearX+'","'+monthX+'");');
            console.log("[+] Month "+monthX+" added on the slider");
        }
        //else {console.log("[-] Month "+monthX+" already exists in the slider");}
    };
}

GeoImage.addDatesToSlider = function(year,month) {
  //actualizo el path con el mes seleccionado
  let pathMonth = document.querySelector('.path .month');
  let monthString = monthNames[month-1];
  pathMonth.innerText = " > " +  monthString.substring(0,3);
  pathMonth.setAttribute('current-month',month);
  //elimino elementos del slider hasta ese momento
  GeoImage.cleanSlider();
  // agrego las fechas que tengan ese mes y año
  let dates = Object.keys(GeoImage.resources); // ((las keys de este array son las fechas))
  for (i in dates){
      let dateX = new Date(dates[i]);
      let yearX = dateX.getFullYear();
      let monthX = dateX.getMonth()+1; // libreria moment() cuenta los meses de 0 a 11
      let dayX = dateX.getDate();// libreria moment() cuenta los dias desde 0
      if ((month==monthX)&(year==yearX)){
          let dayExist = document.querySelector('#results-dates-100 .day a[id="'+dates[i]+'"]');
          if (dayExist == null){
                let list = document.getElementById('results-dates-100');
                let li = document.createElement('div');
                li.classList.add('day');
                let a = document.createElement('a');
                a.setAttribute('id', dates[i]);
                a.setAttribute('year', yearX);
                a.setAttribute('month', monthX);
                a.setAttribute('day', dayX);
                a.innerText = dayX;
                a.setAttribute('onclick', 'GeoImage.loadMapCentralized(\"'+dates[i]+'\");');
                a.classList.add('pop-up');
                //marco las fechas de imagenes que todavia no llegaron.
                if (GeoImage.Layers[GeoImage.resources[dates[i]]['geoIdRaw']] == null){
                  a.classList.add('no-recieved');
                }
                li.appendChild(a);
                list.appendChild(li);
                console.log("[+] Date "+dates[i]+" added on the slider");
          }
      }
  }
}


GeoImage.resetSlider = function() {
  if (GeoImage.initSlider_status) {
    let div = document.getElementById('geometry-type-100');
    div.setAttribute('style', 'display: block');
    let ol = document.getElementById('results-dates-100');
    ol.parentNode.removeChild(ol);
    GeoImage.initSlider_status = false;
  }
}


GeoImage.ShowLayerIfChecked = function(filter, opacity) {
  let id = 'geoId_'+filter
  if (document.getElementById(filter+"-resource").checked) {
      for (let item of GeoImage.resources[GeoImage.selectedGroup]){
          if (GeoImage.Layers[item[id]] != null){
            GeoImage.Layers[item[id]].setVisible(true);
            GeoImage.Layers[item[id]].setOpacity(opacity);
          }
      }
  }
}


GeoImage.loadMapCentralized = function (group) {
  console.log('[+] Image with date '+group+' selected to visualized');
  //Actualiza el path con el dia seleccionado
  let dateGroup = new Date(group);
  let year = dateGroup.getFullYear();
  let month = dateGroup.getMonth()+1;
  let day = dateGroup.getDate();
  let pathDay = document.querySelector('.path .day');
  pathDay.innerText = " > " + day;
  pathDay.setAttribute('current-day',day);

  let OldGroup = document.getElementById(GeoImage.selectedGroup);
  if (OldGroup != null) OldGroup.classList.remove('selected-group');
  GeoImage.selectedGroup = group || GeoImage.selectedGroup;
  let NewGroup = document.getElementById(GeoImage.selectedGroup);
  NewGroup.classList.add('selected-group');
  for (var layers of Object.values(GeoImage.Layers)){
       layers.setVisible(false);
       layers.setOpacity(1);
  }
  if (document.getElementById("raw-resource").checked) {
      for (let item of GeoImage.resources[GeoImage.selectedGroup]){
          if (GeoImage.Layers[item['geoIdRaw']] != null){
            GeoImage.Layers[item['geoIdRaw']].setVisible(true);
          }
      }
  }
  GeoImage.ShowLayerIfChecked('deforestation', 1);
  GeoImage.ShowLayerIfChecked('drought', 1);
  GeoImage.ShowLayerIfChecked('flooding', 1);

  // Cambiamos opacidad
  if (document.getElementById("raw-resource").checked &&
        (document.getElementById("deforestation-resource").checked ||
        document.getElementById("drought-resource").checked ||
        document.getElementById("flooding-resource").checked)) {
      for (let item of GeoImage.resources[GeoImage.selectedGroup]){
        GeoImage.ShowLayerIfChecked('deforestation', 0.5);
        GeoImage.ShowLayerIfChecked('drought', 0.5);
        GeoImage.ShowLayerIfChecked('flooding', 0.5);
        if (GeoImage.Layers[item['geoIdRaw']] != null){
          GeoImage.Layers[item['geoIdRaw']].setOpacity(0.5);
        }
      }
  }
  GeoImage.Map.getLayers().forEach(layer => layer.getSource().refresh());
}

// Consulta el rate de peticiones a la API cada 10 segundos ----------------------------------------------------------------------
GeoImage.getRate = function () {
  setInterval(ajaxCallGetRate, 10000); //Cada 10 segundos
}

function ajaxCallGetRate() {
    let url = GeoImage.baseURL + "/rate";
    Ajax.request( "GET", url, {}, function (xhr) {
                    // LLEGA EL RECURSO Y SE AGREGA LA CAPA.
                      var obj = JSON.parse(xhr);
                      if (xhr === "{}") {
                          console.log("[!] Rate Object are NOT available. Request: " + url);
                      } else {
                          console.log(" Rate Object available. RateLimitRemaining: " + obj['RateLimitRemaining']);
                          e = document.getElementById('rate-api');
                          e.innerHTML = '';
                          h = document.createElement('h4');
                          h.innerText =  "Rate: " + obj['RateLimitRemaining'].toString() +"/"+ obj['RateLimit'].toString();
                          e.appendChild(h);
                      }
    });
}

//-------------------------------------------------------------------------------------------------------------------------------
// Funcion en desuso
GeoImage.intersecPolygons = function (pol1, pol2) {
  var res = turf.polygon(pol1);
  var multi = turf.multiPolygon(pol2);
  var poligons = multi.geometry.coordinates.map(c => turf.polygon(c));
  for (var p of poligons) {
    try {
        let tmp = turf.intersect(res, p);
        res = (tmp != null) ? tmp : res;
    }  catch(error) {
      console.log(error);
      return multi;
    }
  }
  return res;
}

// Proceso Inicial -----------------------------------------------------------------------------
GeoImage.init = function (baseURL) {

  GeoImage.baseURL = baseURL || GeoImage.baseURL;
  GeoImage.Layers = {};
  GeoImage.resources = {};
  GeoImage.color = {};
  GeoImage.resourceClientQueue = ""
  GeoImage.resultClientQueue = ""
  GeoImage.initSlider_status = false;
  GeoImage.slider_active = false;
  //console.log('La fecha es > '+ moment().format('YYYY MM DD')); //prueba libreria moment.
  console.log("Setting baseURL to" + GeoImage.baseURL);
  GeoImage.initClient();
  GeoImage.getRate();

  var raster = new TileLayer({
    source: new OSM()
  });

  var source = new VectorSource({wrapX: false});

  var vector = new VectorLayer({
    source: source
  });

  GeoImage.style = new Style({
    text: new Text({
     font: '12px Calibri,sans-serif',
     fill: new Fill({
       color: '#000'
     }),
     stroke: new Stroke({
       color: '#fff',
       width: 3
     })
    })
  });


  source.on('addfeature', function(evt){
    GeoImage.feature = evt.feature;
    let newCoords = GeoImage.feature.getGeometry().getCoordinates()[0].map(
      function (co) {
        return [Math.floor(co[0] * 1000) / 1000, Math.floor(co[1] * 1000) / 1000];
      }
    );
    GeoImage.feature.getGeometry().setCoordinates([newCoords]);
    let coordConverted = newCoords.map( function(co) {
        return {'latitude': parseFloat(co[1].toString().replace(',','.')), 'longitude': parseFloat(co[0].toString().replace(',','.'))};
    });
    GeoImage.requestNewMap(coordConverted);
  });

  GeoImage.View = new View({
    projection: 'EPSG:4326',
    center: [80.5, 8.2],
    zoom: 11
  });

  GeoImage.Map = new Map({
    layers: [raster, vector],
    target: 'map',
    view: GeoImage.View
  });

  var typeSelect = document.getElementById('type');

  GeoImage.draw = {}; // global so we can remove it later
  function addInteraction() {
    var value = typeSelect.value;
    if (value !== 'None') {
      var geometryFunction;
      if (value === 'Square') {
        value = 'Square';
        geometryFunction = Draw.createRegularPolygon(4);
      } else if (value === 'Box') {
        value = 'Box';
        geometryFunction = Draw.createBox();
      }
      GeoImage.draw = new Draw({
        source: source,
        type: value,
        geometryFunction: geometryFunction
      });
      GeoImage.Map.addInteraction(GeoImage.draw);
    }
  }

  /**
   * Handle change event.
   */
  typeSelect.onchange = function() {
    GeoImage.Map.removeInteraction(GeoImage.draw);
    addInteraction();
  };

  addInteraction();

  var checkboxes = document.querySelectorAll("input[type=checkbox]");

  for (var checkbox of checkboxes) {
    checkbox.addEventListener('change', function() {
        GeoImage.loadMapCentralized();
    });
  }
}
