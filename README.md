## GEO-DIFF

Sistema distribuido de detección de diferencias sobre superficies terrestres.

### Objetivo

Dado un conjunto de imagenes aéreas de una superficie se trata de detectar cambiós sobre la misma.

Estos 'cambios', los denominaremos **metodos de detección**, entre los cuales definiremos:

- Detección de Deforestación.
- Detección de Urbanización.
- Detección de Retroceso del nivel del mar.
- Detección de Sequias.

### Arquitectura del Sistema

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

###### OpenCV

[Instalar OpenCV para Java](https://opencv-java-tutorials.readthedocs.io/en/latest/01-installing-opencv-for-java.html)

##### Arquitectura

###### RabbitMQ	  

[Instalar RabbitMQ](https://www.rabbitmq.com/download.html)
