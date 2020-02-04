


import datetime
import ee

ee.Initialize()

# Create a Landsat 7, median-pixel composite for Spring of 2000.
collection = (ee.ImageCollection('LANDSAT/LE07/C01/T1')
              .filterDate(datetime.datetime(2001, 4, 1),
                          datetime.datetime(2001, 7, 1)))
image1 = collection.median()

# Select the red, green and blue bands.
image = image2.select('B3', 'B2', 'B1')


path = image1.getDownloadUrl({
    'scale': 30,
    'crs': 'EPSG:4326',
    'region': '[[-120, 35], [-119, 35], [-119, 34], [-120, 34]]'
})
print(path)
