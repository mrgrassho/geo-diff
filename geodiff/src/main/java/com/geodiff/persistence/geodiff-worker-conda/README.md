## GEODIFF WORKER

### Dependencias

- convert (imagemagick)
- potrace
- python packages:
    - geopandas (y todas sus depencias)

### ¿Como se realiza la operatoria?

Partimos de una imagen `.png` que devuelve la API de google earth engine la cual
debemos convertir a formato `.bmp`.

```
convert map.png  -resize 50% map.bmp
```

Luego con `potrace` la transformamos en Geojson.
```
./potrace -b geojson --progress -i map.bmp -o map.geojson
```

Luego utilizamos `geopandas` para reducir el tamaño y ajustar las coordenadas
del vector.

```
import geopandas
from shapely.geometry.multipolygon import MultiPolygon
df = geopandas.read_file("./map.geojson")
m = MultiPolygon([df.loc[i].geometry for i in range(df.size)])
g = geopandas.GeoSeries([m])
g = g.scale(0.0001,0.0001,origin=(-0.15981674194335935, 51.499758))
g.to_file("map_resized_multi.geojson", driver='GeoJSON')
quit()
```
