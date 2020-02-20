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

GeoImage.buildNewMapURL = function (beginDate, endDate) {
  beginDate = beginDate == null ? '': beginDate;
  endDate = endDate == null ? '': endDate;
  return GeoImage.baseURL + "/new-map?" +
               "begin-date=" + beginDate + "&" +
               "end-date=" + endDate + "&"+
               "resource_queue=" + GeoImage.resourceClientQueue + "&" +
               "results_queue=" + GeoImage.resultClientQueue + "&";
}

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


GeoImage.requestNewMap = function (coords) {
  // LLAMADA AJAX PARA LA IMAGEN;
  let beginDate = document.getElementById('begin-date').value;
  let endDate = document.getElementById('end-date').value;
  let url = GeoImage.buildNewMapURL(beginDate, endDate);
  Ajax.request("POST", url, coords, function (xhr) {
    console.log(xhr);
  });
}

GeoImage.getDateGroup =  function(date_str) {
  let day = (parseInt(date_str.split('T')[0].slice(-2)) < 16) ? 1 : 16;
  return date_str.split('T')[0].slice(0, 8) + day
}

GeoImage.onResourceMessage = function(d) {
  let geoImage = JSON.parse(d.body);
  let group = GeoImage.getDateGroup(geoImage["earthImage"]["date"]);
  if (GeoImage.resources[group] == null) {
    GeoImage.resources[group] = [];
    GeoImage.color[group] = GeoImage.getRandomColor();
  }
  geoImage["geoIdVector"]  = GeoImage.getHashCode(geoImage, "VECTOR");
  geoImage["geoIdRaw"]  = GeoImage.getHashCode(geoImage, "RAW");
  GeoImage.resources[group].push(geoImage);
  GeoImage.initSlider();
  GeoImage.addElementToSlider(group);
}

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

//GeoImage.resultVector = function(item) {
//  let geoIdVector = GeoImage.getHashCode(item, "VECTOR");
//  let group = GeoImage.getDateGroup(item["earthImage"]["date"]);
//  //let multiPolygon = GeoImage.intersecPolygons(GeoImage.feature.getGeometry().getCoordinates(), (new GeoJSON()).readFeatures(item["vectorImage"])[0].getGeometry().getCoordinates());
//  let multiPolygon = item["vectorImage"];
//  GeoImage.Layers[geoIdVector] = new VectorLayer({
//    source: new VectorSource({
//      features: (new GeoJSON()).readFeatures(multiPolygon)
//    }),
//     style: function(feature) {
//       let style = GeoImage.style.clone();
//       style.setFill( new Fill({
//          color: GeoImage.color[group]
//       }));
//       return style;
//     }
//  });
//  GeoImage.Map.addLayer(GeoImage.Layers[geoIdVector]);
//  GeoImage.Layers[geoIdVector].setVisible(false);
//}

GeoImage.resultVector = function(item) {
  let offset = item["earthImage"]["dim"] / 2;
  let minLatLon = [item["earthImage"]["coordinate"]['longitude']-offset, item["earthImage"]["coordinate"]['latitude']-offset];
  let maxLatLon = [item["earthImage"]["coordinate"]['longitude']+offset, item["earthImage"]["coordinate"]['latitude']+offset];
  let geoIdRaw = GeoImage.getHashCode(item, "VECTOR");
  GeoImage.Layers[geoIdRaw] = new ImageLayer({
    source: new Static({
      imageLoadFunction : function(image){ image.getImage().src = item["vectorImage"]; },
      crossOrigin: '',
      imageExtent: [minLatLon[0], minLatLon[1], maxLatLon[0], maxLatLon[1]]
    })
  });
  GeoImage.Map.addLayer(GeoImage.Layers[geoIdRaw]);
  GeoImage.Layers[geoIdRaw].setVisible(false);
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

GeoImage.onResultMessage = function(d) {
  let geoImage = JSON.parse(d.body);
  let group = GeoImage.getDateGroup(geoImage["earthImage"]["date"]);
  console.log(" [+] Resource available. GeoImage:" + geoImage);
  if (Object.entries(GeoImage.Layers).length === 0 && GeoImage.Layers.constructor === Object) {
    GeoImage.selectedGroup = group;
  }
  GeoImage.resultVector(geoImage);
  GeoImage.resultImage(geoImage);
  if (group == GeoImage.selectedGroup) {
    GeoImage.loadMapCentralized(group)
  }
}

GeoImage.onConnectRabbitMQ = function(x) {
  GeoImage.client.subscribe(GeoImage.resourceClientQueue, GeoImage.onResourceMessage, {'auto-delete': true});
  GeoImage.client.subscribe(GeoImage.resultClientQueue,  GeoImage.onResultMessage, {'auto-delete': true});
}

GeoImage.onErrorRabbitMQ =  function(frame) {
  alert("Request coordinates are NOT available.");
  console.log(' [!] Error: ', frame);
  let map = document.getElementById('map');
  map.innerHTML = '';
  GeoImage.init();
};

GeoImage.initRabbitMQ =  function() {
  GeoImage.client = Stomp.client('ws://' + window.location.hostname + ':15674/ws');
  GeoImage.client.connect(GeoImage.rabbitMQ.u, GeoImage.rabbitMQ.p , GeoImage.onConnectRabbitMQ, GeoImage.onErrorRabbitMQ, '/');
}

GeoImage.getRandomColor = function () {
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

GeoImage.initSlider = function() {
  if (!GeoImage.initSlider_status) {
    let list = document.getElementById('result-groups-100');
    let div = document.getElementById('geometry-type-100');
    div.setAttribute('style', 'display: none');
    let ol = document.createElement('div');
    ol.setAttribute('id', 'results-dates-100');
    list.appendChild(ol);
    GeoImage.initSlider_status = true;
  }
}

GeoImage.addElementToSlider = function(groupDate) {
  if (document.getElementById(groupDate) == null) {
    let list = document.getElementById('results-dates-100');
    let li = document.createElement('div');
    let a = document.createElement('a');
    a.setAttribute('id', groupDate);
    a.innerText = groupDate;
    a.setAttribute('onclick', 'GeoImage.loadMapCentralized(\"'+groupDate+'\");')
    a.classList.add('pop-up');
    li.appendChild(a);
    let elem = null
    for (let child of list.childNodes) {
      if (child.getElementsByTagName('A')[0].id.localeCompare(a.id) < 0) {
        elem = child;
      } else {
        break;
      }
    }
    list.insertBefore(li, elem);
  }
}

GeoImage.loadMapCentralized = function (group) {
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
  if (document.getElementById("vector-resource").checked) {
      for (let item of GeoImage.resources[GeoImage.selectedGroup]){
          if (GeoImage.Layers[item['geoIdVector']] != null){
            GeoImage.Layers[item['geoIdVector']].setVisible(true);
            GeoImage.Layers[item['geoIdVector']].setOpacity(0.7);
          }
      }
  }
  // Cambiamos opacidad
  if (document.getElementById("raw-resource").checked && document.getElementById("vector-resource").checked) {
      for (let item of GeoImage.resources[GeoImage.selectedGroup]){
        if (GeoImage.Layers[item['geoIdVector']] != null){
          GeoImage.Layers[item['geoIdVector']].setOpacity(0.5);
        }
        if (GeoImage.Layers[item['geoIdRaw']] != null){
          GeoImage.Layers[item['geoIdRaw']].setOpacity(0.5);
        }
      }
  }
  GeoImage.Map.getLayers().forEach(layer => layer.getSource().refresh());
}

GeoImage.init = function (baseURL) {

  GeoImage.baseURL = baseURL || GeoImage.baseURL;
  GeoImage.Layers = {};
  GeoImage.resources = {};
  GeoImage.color = {};
  GeoImage.resourceClientQueue = ""
  GeoImage.resultClientQueue = ""
  GeoImage.initSlider_status = false;
  console.log("Setting baseURL to " + GeoImage.baseURL);
  GeoImage.initClient();

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
        return {'latitude': co[1], 'longitude': co[0]};
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
