## GEO-DIFF

Sistema distribuido de detección de diferencias sobre superficies terrestres.

### Objetivo

Dado un conjunto de imagenes aéreas de una superficie se trata de detectar cambiós sobre la misma.

Estos 'cambios', los denominaremos **metodos de detección**, entre los cuales definiremos:

- **Detección de Deforestación.**
- ~~Detección de Urbanización.~~
- ~~Detección de Retroceso del nivel del mar.~~
- ~~Detección de Sequias.~~

### Mockups

#### Home

![Home](/diagrams/GeoDiff-Home.png)

#### Resultados

![Resultados](/diagrams/GeoDiff-Resultados.png)

### Arquitectura del Sistema

![Arquitectura](/diagrams/GeoDiff-DiagramaDeArquitectura.png)

1. Desde el frontEnd se envia las coordenadas del mapa dibujado. (+ Junto con el periodo de fechas que se quiere buscar)
2. Se solicitian Imagenes a la API ([NASA - Earth](https://api.nasa.gov/api.html#earth))
3. Se encolan los resultados en la Work Queue.
4. Los workers consumen de la work queue y aplican el filtro de detección.
5. Los resultados son encolados en la Result Queue.
6. El Client lee imagenes del Result Queue y los escribe (7) guarda en un directorio temporal. (TO-DO: Analizar si vale la pena utilizar un BD para almacenar los resultados)
8. Devuelven los resultados al frontEnd (Se encarga de posicionar los resultados sobre el mapa).

#### Nodos

- Master: Nodo encargado de la segmentación y distribución de los conjuntos de imagenes
- Workers: Nodo encargado de realizar el proceso elegido.

#### Operatoria

1. El Master segmentará los conjuntos de imagenes en partes iguales y  los enviará a una cola de proceso.
2. El Worker tomará de la cola de proceso subconjunto de imagenes y aplicará la función/es (metodo de detección) elegida.
3. El Worker generará una nueva imagen marcando las zonas detectadas con diferentes colores (para cada metodo de detección) y luego la enviará a una cola de resultados.
4. El Master se encargará de unir las imagenes resultantes.

#### Herramientas

##### Procesamiento de Imagenes

Para el proceso de imagenes se eligió **OpenCV**

[Instalar OpenCV para Java](https://opencv-java-tutorials.readthedocs.io/en/latest/01-installing-opencv-for-java.html)

##### Imágenes Satelitales - (Lista de APIs abiertas)

A continuación se listan APIs de imagenes satelitales a investigar/probar.

- [NASA - Earth](https://api.nasa.gov/api.html#earth).

  - Utiliza satelites Landsat.
  - API con limite de 1000 peticiones por hora.


##### Arquitectura

###### RabbitMQ	  

[Instalar RabbitMQ](https://www.rabbitmq.com/download.html)

##### PLAN

- [X] Armar ejemplos en OpenCV
- [ ] Armar algoritmos de detección en OpenCV
- [ ] Conseguir Datasets de Imagenes
- [X] Plantear Arquitectura
- [X] Definir que va a tener la Interfaz Web
