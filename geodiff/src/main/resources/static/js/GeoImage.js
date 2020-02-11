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
var proj = ol.proj;
var Ajax = Ajax;


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

GeoImage.buildImgAssets = function (beginDate, endDate) {
  beginDate = beginDate == null ? '': beginDate;
  endDate = endDate == null ? '': endDate;
  return GeoImage.baseURL + "/img-assets?" +
               "begin-date=" + beginDate + "&" +
               "end-date=" + endDate;
}

GeoImage.getAssets = function (coords) {
  // LLAMADA AJAX PARA LA IMAGEN;
  let beginDate = document.getElementById('begin-date').value;
  let endDate = document.getElementById('end-date').value;
  let url = GeoImage.buildImgAssets(beginDate, endDate);
  Ajax.request( "POST", url, coords, function (xhr) {
  // LLEGA EL RECURSO Y SE AGREGA LA CAPA.
    if (xhr == {} || xhr == null) {
        console.log('[!] No Results Found. Request: ' + url + " Body: " + coords)
    } else  {
        console.log('Add Image Layer :', xhr)
        GeoImage.Main(xhr);
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

GeoImage.buildSlider = function() {
  let list = document.getElementById('result-groups-100');
  list.innerHTML = '';
  let ol = document.createElement('div');
  for (var i of Object.keys(GeoImage.resources)) {
    let li = document.createElement('div');
    let a = document.createElement('a');
    // Muestro solo la primer fecha de cada grupo
    a.innerText = GeoImage.resources[i][0]['date'].split("T")[0];
    a.setAttribute('onclick', 'GeoImage.loadMap('+i+');')
    a.classList.add('pop-up');
    li.appendChild(a);
    ol.appendChild(li);
  }
  list.appendChild(ol);
}

GeoImage.loadMap = function (id) {
  let group = GeoImage.resources[id];
  for (var item of group) {
    GeoImage.getImg(item['date'], item['coordinate'], 'RAW');
  }
}

GeoImage.Main = function (resources) {
  GeoImage.resources = resources;
  let lat = GeoImage.resources[0][0]["coordinate"]["latitude"];
  let lon = GeoImage.resources[0][0]["coordinate"]["longitude"];
  GeoImage.Map.view.center = proj.fromLonLat([lon, lat]);
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

  source.on('addfeature', function(evt){
    var feature = evt.feature;
    var coords = feature.getGeometry().getCoordinates()[0];
    var coordConverted = [];
    for (co of coords) {
      let coord = {'latitude':0,'longitude':0};
      console.log(co);
      coord['latitude'] = proj.transform(co, 'EPSG:3857', 'EPSG:4326')[1];
      coord['longitude'] = proj.transform(co, 'EPSG:3857', 'EPSG:4326')[0];
      coordConverted.push(coord);
    }
    GeoImage.getAssets(coordConverted);
  });

  GeoImage.Map = new Map({
    layers: [raster, vector],
    target: 'map',
    view: new View({
      center: proj.fromLonLat([-8.625, 18.3]),
      zoom: 11
    })
  });

  var typeSelect = document.getElementById('type');

  var draw; // global so we can remove it later
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
      draw = new Draw({
        source: source,
        type: value,
        geometryFunction: geometryFunction
      });
      GeoImage.Map.addInteraction(draw);
    }
  }


  /**
   * Handle change event.
   */
  typeSelect.onchange = function() {
    GeoImage.Map.removeInteraction(draw);
    addInteraction();
  };

  addInteraction();
}
