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
               "end-date=" + endDate;
}

GeoImage.requestNewMap = function (coords) {
  // LLAMADA AJAX PARA LA IMAGEN;
  let beginDate = document.getElementById('begin-date').value;
  let endDate = document.getElementById('end-date').value;
  let url = GeoImage.buildNewMapURL(beginDate, endDate);
  Ajax.request( "POST", url, coords, function (xhr) {
  // LLEGA EL RECURSO Y SE AGREGA LA CAPA.
    var obj = JSON.parse(xhr);
    console.log('Fucking Object :', obj)
    if (xhr === "{}") {
        console.log("[!] Request Object are NOT available. Request: " + url + " Body: " + JSON.stringify(coords));
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

GeoImage.getDateGroup =  function(date_str) {
  let day = (parseInt(date_str.split('T')[0].slice(-2)) < 16) ? 1 : 16;
  return date_str.split('T')[0].slice(0, 8) + day
}

GeoImage.onResourceMessage = function(d) {
  let geoImage = JSON.parse(d.body);
  let group = GeoImage.getDateGroup(geoImage["earthImage"]["date"]);
  if (GeoImage.resources[group] == null) {
    GeoImage.resources[group] = [];
  }
  geoImage["geoId"]  = GeoImage.getHashCode(geoImage);
  GeoImage.resources[group].push(geoImage);
  GeoImage.initSlider();
  GeoImage.addElementToSlider(group);
}

GeoImage.resultVector = function(item) {
  let geoId = GeoImage.getHashCode(item);
  item["geoId"] = geoId;
  let color = GeoImage.getRandomColor();
  GeoImage.Layers[geoId] = new VectorLayer({
    source: new VectorSource({
      features: (new GeoJSON()).readFeatures(item["vectorImage"])
    }),
     style: function(feature) {
       GeoImage.style.getText().setText(feature.get('name'));
       GeoImage.style.setFill( new Fill({
          color: color
       }));
       return GeoImage.style;
     }
  });
  GeoImage.Map.addLayer(GeoImage.Layers[geoId]);
  GeoImage.Layers[geoId].setVisible(false);
}

GeoImage.resultImage = function(item) {
  let minLatLon = [item["earthImage"]["coordinate"]['latitude']-GeoImage.offset, item["earthImage"]["coordinate"]['longitude']-GeoImage.offset];
  let maxLatLon = [item["earthImage"]["coordinate"]['latitude']+GeoImage.offset, item["earthImage"]["coordinate"]['longitude']+GeoImage.offset];
  let geoId = GeoImage.getHashCode(item);
  item["geoId"] = geoId;
  GeoImage.Layers[geoId] = new ImageLayer({
    source: new Static({
      imageLoadFunction : function(image){ image.getImage().src = item["earthImage"]["rawImage"]; },
      crossOrigin: '',
      imageExtent: [minLatLon[0], minLatLon[1], maxLatLon[0], maxLatLon[1]]
    })
  });
  GeoImage.Map.addLayer(GeoImage.Layers[geoId]);
  GeoImage.Layers[geoId].setVisible(false);
}

GeoImage.onResultMessage = function(d) {
  let geoImage = JSON.parse(d.body);
  let group = GeoImage.getDateGroup(geoImage["earthImage"]["date"]);
  console.log(" [+] Resource available. GeoImage:" + geoImage);
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
    list.appendChild(li);
  }
}

GeoImage.loadMapCentralized = function (group) {
  GeoImage.selectedGroup = group;
  for (var layers of Object.values(GeoImage.Layers)){
       layers.setVisible(false);
  }
  if (document.getElementById("raw-resource").checked) {
      for (let item of GeoImage.resources[group]){
        if (GeoImage.Layers[item['geoId']] != null){
          GeoImage.Layers[item['geoId']].setVisible(true);
        }
      }
  }
  if (document.getElementById("vector-resource").checked) {
    for (let item of GeoImage.resources[group]){
      if (GeoImage.Layers[item['geoId']] != null){
        GeoImage.Layers[item['geoId']].setVisible(true);
      }
    }
  }
  // Cambiamos opacidad
  if (document.getElementById("raw-resource").checked && document.getElementById("vector-resource").checked) {
      for (let item of GeoImage.resources[group]){
        if (GeoImage.Layers[item['geoId']] != null){
          GeoImage.Layers[item['geoId']].setOpacity(0.5);
        }
      }
  }
  GeoImage.Map.getLayers().forEach(layer => layer.getSource().refresh());
}

GeoImage.init = function (offset, baseURL) {
  var raster = new TileLayer({
    source: new OSM()
  });

  GeoImage.offset  = offset || GeoImage.offset;
  GeoImage.offset = GeoImage.offset / 2;
  GeoImage.baseURL = baseURL || GeoImage.baseURL;
  GeoImage.Layers = {};
  GeoImage.resources = {};
  GeoImage.resourceClientQueue = ""
  GeoImage.resultClientQueue = ""
  GeoImage.initSlider_status = false;
  console.log("Setting baseURL to " + GeoImage.baseURL);

  var source = new VectorSource({wrapX: false});

  var vector = new VectorLayer({
    source: source
  });

  GeoImage.style = new Style({
    stroke: new Stroke({
     color: '#319FD3',
     width: 1
    }),
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
    var feature = evt.feature;
    var coords = feature.getGeometry().getCoordinates()[0];
    var coordConverted = [];
    for (co of coords) {
      let coord = {'latitude':0,'longitude':0};
      coord['latitude'] = co[1];
      coord['longitude'] = co[0];
      coordConverted.push(coord);
    }
    GeoImage.requestNewMap(coordConverted);
  });

  GeoImage.View = new View({
    projection: 'EPSG:4326',
    center: [80.5, 8.2],
    zoom: 9
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
}
