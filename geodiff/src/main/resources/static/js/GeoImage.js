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

GeoImage.buildImgURL = function (date, lat, lon, filter) {
  date = date == null ? '': date;
  lat = lat == null ? '': lat;
  lon = lon == null ? '': lon;
  filter = filter == null? '': filter;
  return GeoImage.baseURL + "/img?" +
               "date=" + date + "&" +
               "lat=" + lat + "&" +
               "lon=" + lon + "&" +
               "filter=" + filter;
}

GeoImage.buildVectorURL = function (date, lat, lon, filter) {
  return GeoImage.baseURL + "/vector?" +
             "date=" + date + "&" +
             "lat=" + lat + "&" +
             "lon=" + lon + "&" +
             "filter=" + filter;
}

GeoImage.buildImgAssets = function (beginDate, endDate) {
  beginDate = beginDate == null ? '': beginDate;
  endDate = endDate == null ? '': endDate;
  return GeoImage.baseURL + "/img-assets?" +
               "begin-date=" + beginDate + "&" +
               "end-date=" + endDate;
}

GeoImage.getRandomColor = function () {
  var letters = '0123456789ABCDEF';
  var color = '#';
  for (var i = 0; i < 6; i++) {
    color += letters[Math.floor(Math.random() * 16)];
  }
  return Color.asArray(color);
}

GeoImage.getAssets = function (coords) {
  // LLAMADA AJAX PARA LA IMAGEN;
  let beginDate = document.getElementById('begin-date').value;
  let endDate = document.getElementById('end-date').value;
  let url = GeoImage.buildImgAssets(beginDate, endDate);
  Ajax.request( "POST", url, coords, function (xhr) {
  // LLEGA EL RECURSO Y SE AGREGA LA CAPA.
    var obj = JSON.parse(xhr);
    console.log('Fucking Object :', obj)
    if (xhr === "{}") {
        console.log("[!] Request coordinates are NOT available. Request: " + url + " Body: " + JSON.stringify(coords));
        alert("Request coordinates are NOT available.");
        GeoImage.dropLastLayer();
    } else  {
        console.log('Add Image Layer :', obj)
        GeoImage.Main(obj);
    }
  });
}

GeoImage.getImg = function(date, coord, filter) {
  let minLatLon = proj.fromLonLat([coord['longitude']+GeoImage.offset, coord['latitude']-GeoImage.offset]);
  let maxLatLon = proj.fromLonLat([coord['longitude']-GeoImage.offset, coord['latitude']+GeoImage.offset]);
  let url =  GeoImage.buildImgURL(date, coord['latitude'], coord['longitude'], filter);
  // LLAMADA AJAX PARA LA IMAGEN;
  Ajax.request( "GET", url, {}, function (xhr) {
  // LLEGA EL RECURSO Y SE AGREGA LA CAPA.
    if (xhr != null) {
      GeoImage.Map.addLayer(
        new ImageLayer({
          source: new Static({
            imageLoadFunction : function(image){ image.getImage().src = xhr; },
            crossOrigin: '',
            projection: 'EPSG:3857',
            imageExtent: [minLatLon[0], minLatLon[1], maxLatLon[0], maxLatLon[1]]
          })
        })
      );
    }
  });
}

GeoImage.getVector = function(date, lat, lon, filter) {
  let url =  GeoImage.buildVectorURL(date, lat, lon, filter);
  // LLAMADA AJAX PARA LA IMAGEN;
  Ajax.request( "GET", url, {}, function (xhr) {
  // LLEGA EL RECURSO Y SE AGREGA LA CAPA.
    if (xhr != null && xhr !== "") {
      let geojsonObject = JSON.parse(xhr);
      console.log(" [+] Resource available. URL:"+ url);
      GeoImage.Map.addLayer(
        new VectorLayer({
          source: new VectorSource({
            features: (new GeoJSON()).readFeatures(geojsonObject)
          }),
           style: function(feature) {
             GeoImage.style.getText().setText(feature.get('name'));
             GeoImage.style.setFill( new Fill({
                color: GeoImage.getRandomColor()
             }));
             return GeoImage.style;
           }
        })
      );
    } else {
        console.log(" [!] Resource NOT available. URL:"+ url);
    }
  });
}

GeoImage.buildSlider = function() {
  let list = document.getElementById('result-groups-100');
  list.innerHTML = '';
  let ol = document.createElement('div');
  let sortedKeys = Object.keys(GeoImage.resources).sort();
  for (var i of sortedKeys) {
    let li = document.createElement('div');
    let a = document.createElement('a');
    a.innerText = i;
    a.setAttribute('onclick', 'GeoImage.loadMapCentralized(\"'+i+'\");')
    a.classList.add('pop-up');
    a.classList.add('green-btn');
    li.appendChild(a);
    ol.appendChild(li);
  }
  list.appendChild(ol);
}

GeoImage.dropLastLayer = function() {
    let lastLayer = GeoImage.Map.getLayers()["array_"].pop();
    if (lastLayer != null) GeoImage.Map.removeLayer(lastLayer);
}

GeoImage.loadMapCentralized = function (id) {

    if (document.getElementById("raw-resource").checked) {
        GeoImage.loadMap(id);
    }

    if (document.getElementById("vector-resource").checked) {
        GeoImage.loadMapVector(id);
    }
}

GeoImage.loadMap = function (id) {
  let group = GeoImage.resources[id];
  let lat = group[0]['centerCoordinate']["latitude"];
  let lon = group[0]['centerCoordinate']["longitude"];
  //GeoImage.View.setCenter([lat, lon]);
  for (var item of group) {
    GeoImage.getImg(item['date'], item['centerCoordinate'], 'RAW');
  }
}

GeoImage.loadMapVector = function (id) {
  let group = GeoImage.resources[id];
  let lat = group[0]['centerCoordinate']["latitude"];
  let lon = group[0]['centerCoordinate']["longitude"];
  //GeoImage.View.setCenter([lat, lon]);
  for (var item of group) {
    GeoImage.getVector(item['date'], item['centerCoordinate']["latitude"], item['centerCoordinate']["longitude"], 'RAW');
  }
}

GeoImage.Main = function (resources) {
    GeoImage.resources = resources;
    GeoImage.buildSlider();
}

GeoImage.init = function (offset, baseURL) {
  var raster = new TileLayer({
    source: new OSM()
  });

  console.log("Setting baseURL to " + baseURL);
  GeoImage.offset  = offset;
  GeoImage.baseURL = baseURL;

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
      console.log(co);
      coord['latitude'] = co[1];
      coord['longitude'] = co[0];
      coordConverted.push(coord);
    }
    GeoImage.getAssets(coordConverted);
  });

  GeoImage.View = new View({
      projection: 'EPSG:4326',
      center: [0, 0],
      zoom: 2
  })

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
        value = 'Circle';
        geometryFunction = Draw.createRegularPolygon(4);
      } else if (value === 'Box') {
        value = 'Circle';
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
