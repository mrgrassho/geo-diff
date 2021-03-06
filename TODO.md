### TO-DO:

#### General:

- [X] Armar ejemplos en OpenCV
- [ ] **Armar algoritmos de detección en OpenCV (Basarse en lo ya creado y agregar mas filtros al resultado)**
- [X] Conseguir Datasets de Imagenes (API NASA)
- [X] Plantear Arquitectura
- [X] Definir que va a tener la Interfaz Web (Ver Mockups)
- [X] Armar wrapper a la API (Similar a [python-wrapper]())
- [X] Agrega persistencia a los datasets (MongoDB).
- [X] Decidir que tecnologías usar (back, front, etc).
- [X] Sobre el Front. (Analizar si OpenStreetMap cumple con lo deseado). **Se eligió OpenLayers.**

- [ ] Documentar bien la API y OpenLayers (las mañas que tiene de tipos de notación para puntos de coordenadas -> 'EPSG:4326', 'EPSG:21781')

- [ ] Devolver msj de NO se encontraron resultados, Tener en cuenta que hay puntos geograficos que no devulve resultados o no estan disponibles en periodos de tiempo

#### OpenCV

- [ ] Al filtro de deforestación agregarle mas gamas de procesos. No un único color para diferenciar campos de tierra.
- [ ] Hacer un proceso más fino sobre el filtro de deforestación para diferenciar entre bosques/selvas y plantaciones. **Se puede agregar lógica según la forma del terreno, es facil distinguir terrenos siguiendo esto.**
- [ ] Definir logica para filtros de sequia y edificación.

#### FrontEnd

- [ ] Ver la forma de obtener las coordenadas que se encuentran dentro de un area marcada con OpenLayers.
- [X] Agregar Slider por fechas (definir de cuantos días va a ser la agrupación). *Recordar que no todas las imagenes obtenidas van a ser del mismo día.*
- [X] Armar clases JS para posicionar y obtener las imagenes procesadas.
- [ ] Realizar checkbox de los filtros dinamicamente. (**Eliminar Hardcode**).

#### RabbitMQ

- [X] Armar Arquitectura RabbitMQ.

#### MongoDB

- [X] Arreglar Query Regex en MongoRepository (No obtiene bien el resultado).
- [ ] Cambiar la forma de realizar un query sobre una coordenada. (Basarse en una coordenada aproximada, [coord + - una constante]). ALTERNATIVA: Estudiar BDs arcGIS.

## Versión 2.0

#### Worker

- [x] Testear.
- [ ] Reducir tamaño de imagen de Docker (~2.5 GB). Se va al carajo.
- [x] Revisar el metodo que mueve un Polygon que parece que esta al reves (lon, lat) en vez de (lat, lon).
- [ ] Revisar el filtro que aplica el worker

#### Backend

- [x] Arreglar variables de entorno application.properties para mongobee.
- [x] Testear.
- [x] Revisar CloudScore (no funca el filtro).
- [x] Pensar manera de optimizar el proceso. (Que lo workers descarguen las imagenes. Esta opcion es facil de implementar con las librerias httpclient)
- [ ] Jugar con el tamaño del rectangulo que solicita el cliente. Y realizar una petición de dim = min(longitud del lado del rectangulo).
- [ ] Ajustar la petición de las imagenes para que ocupe todas las coordenadas del rectangulo. Pedir mas resources para abajo del rectangulo.
- [x] NO abrir una Cola cada vez que el cliente dibuja un rectangulo, reutilizar la misma Cola.

#### Mongo
- [x] Revisar container que no esta tomando la configuracion de init-mongo.js

#### FrontEnd

- [ ] Revisar que los divs de fechas se carguen en orden cronologico.
- [ ] Agrupar los divs de fechas por mes/ por cuatrimestre / por año para evitar llenar de botones.
