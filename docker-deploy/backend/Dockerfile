FROM adoptopenjdk/openjdk8:latest

RUN mkdir /usr/src/boot
WORKDIR /usr/src/boot

COPY application.properties ./config/

COPY geodiff-0.0.1-SNAPSHOT.jar .

EXPOSE 8080

CMD ["java", "-jar", "geodiff-0.0.1-SNAPSHOT.jar"]
