import ee
import folium
import datetime
import webbrowser

ee.Initialize()

class eeMapHack(object):
    def __init__(self,center=[0, 0],zoom=3):
        self._map = folium.Map(location=center,zoom_start=zoom)
        return

    def addToMap(self,img,vizParams,name):
         map_id = ee.Image(img.visualize(**vizParams)).getMapId()
         tile_url_template = "https://earthengine.googleapis.com/map/{mapid}/{{z}}/{{x}}/{{y}}?token={token}"
         mapurl = tile_url_template.format(**map_id)
         folium.WmsTileLayer(mapurl,name=name).add_to(self._map)

         return

    def addLayerControl(self):
         self._map.add_child(folium.map.LayerControl())
         return


# initialize map object
eeMap = eeMapHack(center=[18.453,-95.738],zoom=9)

# Filter the LE7 collection to a single date.
collection = (ee.ImageCollection('LE7_L1T')
          .filterDate(datetime.datetime(2002, 11, 8),
                      datetime.datetime(2002, 11, 9)))
image = collection.mosaic().select('B3', 'B2', 'B1')
eeMap.addToMap(image, {'gain': '1.6, 1.4, 1.1'}, 'Land')

# Add and stretch the water.  Once where the elevation is masked,
# and again where the elevation is zero.
elev = ee.Image('srtm90_v4')
mask1 = elev.mask().eq(0).And(image.mask())
mask2 = elev.eq(0).And(image.mask())

eeMap.addToMap(image.mask(mask1), {'gain': 6.0, 'bias': -200}, 'Water: Masked')
eeMap.addToMap(image.mask(mask2), {'gain': 6.0, 'bias': -200}, 'Water: Elev 0')

# add layer control to map
eeMap.addLayerControl()

outHtml = 'map.html' # temporary file path, change if needed
eeMap._map.save(outHtml)

webbrowser.open('file://'+outHtml) 
