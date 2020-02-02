### ¿Que hay que definir?


> ¿Como es el flujo de la aplicación?

1. Dibujo figura (GeoJson) sobre el mapa. (Shift+Mouse). El front va a buscar las coordenadas de todos los puntos dentro de la figura.

  3. El back devuelve lista de coordenadas + fecha a la que pertenece.

4. El front arma el Slider de fechas (intervalo de una semana por si no coinciden las fechas)

  5. El back tira thread que va cacheando / procesando las imagenes pero no impide que el usuario siga dibujando nuevas figuras (¿Como resulvo esto por parte del Slider si ya está armado sobre otro set de imagenes?)

6. El front selecciona por defecto la primer fecha. Va a buscar las imagenes y las pega en el Mapa.

> ¿Como solucionamos el tema del almacenado de imagenes? ¿Dada un coordenada X = (3, 4) cuanto abarca? ¿X = 3, 3 esta contenida dentro de esta?

Posibles soluciones: ver el zoom de la API.
