# GeoDiff

Sistema distribuido de detección de diferencias sobre superficies terrestres.

## Getting Started

A continuación definiremos las instrucciones para poder tener el proyecto corriendo de manera local. Ve a la sección **Instalación**.

### Prerequisitos

Para poder instalar el proyecto primero es necesario instalar [Docker](https://www.docker.com/).

### Instalación

A continuación definiremos los pasos para correr el proyecto.

```
git clone ''
cd /docker-deploy
docker-compose --compatibility up
```

Este último comando instalará las dependecias de los contenedores y los correrá. Para mas información de que hay en los contenedores analice la siguiente tabla:

| Aplicación     | Función     |
| :------------- | :------------- |
| MongoDB        | Base de Datos no relacional      |
| Mongo-Express  | Administrador de MondoDB  |
| RabbitMQ       | Servidor de Mesajeria   |
| Worker         | Procesar tareas   |
| Admin-Worker  | Administrador de workers, garantiza que esten activos  |
| Backend  | Aplicacion principal. Servidor Web Spring Boot  |

## Probando GeoDiff

Una vez deployados los containers. Ir a [localhost:8080/geo](localhost:8080/geo) para comenzar a utilizar la aplicación.

## Documentación

### Arquitectura del Sistema

![Arquitectura](/diagrams/GeoDiff-DiagramaDeArquitectura.png)

### ¿Como funciona?

A continuación describiremos los pasos que realiza la aplicación para realizar una tarea.

> En desarrollo...

## Built With

* [Spring Boot](https://spring.io/projects/spring-boot) - The web framework used
* [Maven](https://maven.apache.org/) - Dependency Management
* [RabbitMQ](https://www.rabbitmq.com/) - Message Broker
* [OpenLayers](https://openlayers.org/) - Used for Map render
* [RabbitMQ STOMP Plugin](https://www.rabbitmq.com/stomp.html) - Used to  communicate over WebSockets

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details
