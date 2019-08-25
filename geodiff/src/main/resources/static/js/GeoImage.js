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
var Ajax = Ajax;

GeoImage.offset = 0.1;
GeoImage.baseURL = "http://localhost:8080/geo";

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

GeoImage.getImg = function(date, coord, filter) {
  let minLatLon = GeoImage.proj.fromLonLat([coord['longitude']+GeoImage.offset, coord['latitude']-GeoImage.offset]);
  let maxLatLon = GeoImage.proj.fromLonLat([coord['longitude']-GeoImage.offset, coord['latitude']+GeoImage.offset]);
  let url =  GeoImage.buildImgURL(date, coord['latitude'], coord['longitude'], filter);
  // LLAMADA AJAX PARA LA IMAGEN;
  Ajax.request( "GET", url, {}, function (xhr) {
  // LLEGA EL RECURSO Y SE AGREGA LA CAPA.
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
  });
}

GeoImage.buildSlider = function() {
  let list = document.getElementById('result-groups-100');
  let ol = document.createElement('div');
  for (var i of Object.keys(GeoImage.resources)) {
    let li = document.createElement('div');
    let a = document.createElement('a');
    // Muestro solo la primer fecha de cada grupo
    a.innerText = GeoImage.resources[i][0]['date'].split("T")[0];
    a.setAttribute('onclick', 'GeoImage.loadMap('+i+');')
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
  GeoImage.proj = ol.proj;
  GeoImage.resources = resources;
  let lat = GeoImage.resources[0][0]["coordinate"]["latitude"];
  let lon = GeoImage.resources[0][0]["coordinate"]["longitude"];

  var raster = new TileLayer({
              source: new OSM()
            });

  var source = new VectorSource({wrapX: false});

            var vector = new VectorLayer({
              source: source
            });

  GeoImage.Map = new Map({
    layers: [raster, vector, new ImageLayer({
                opacity: 0.75,
                source: new Static({
                  url: 'https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/' +
                         'British_National_Grid.svg/2000px-British_National_Grid.svg.png',
                  crossOrigin: '',
                  projection: 'EPSG:3857',
                  imageExtent: [-841259.17, 6405988.47, 404314.67, 8733289.17]
                })
              })
            ],
    target: document.getElementById('map'),
    view: new View({
      center: GeoImage.proj.fromLonLat([lon, lat]),
      projection: 'EPSG:3857',
      zoom: 4
    })
  });
  GeoImage.buildSlider();
}
