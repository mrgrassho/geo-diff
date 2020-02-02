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

- [ ] Armar Arquitectura RabbitMQ.

#### MongoDB

- [X] Arreglar Query Regex en MongoRepository (No obtiene bien el resultado).
- [ ] Cambiar la forma de realizar un query sobre una coordenada. (Basarse en una coordenada aproximada, [coord + - una constante]). ALTERNATIVA: Estudiar BDs arcGIS.
