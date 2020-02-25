FROM adoptopenjdk/openjdk8:latest

RUN mkdir /usr/src/boot
WORKDIR /usr/src/boot

COPY geodiff-0.0.1-SNAPSHOT.jar ./

COPY application.properties ./config/

EXPOSE 8080

CMD ["java", "-jar", "geodiff-0.0.1-SNAPSHOT.jar"]
