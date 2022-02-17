FROM gradle:latest as BUILD

COPY . /source

WORKDIR /source
RUN gradle build

FROM openjdk:19-jdk-alpine as RUN

RUN mkdir /db
WORKDIR app
COPY --from=BUILD /source/build/libs/reader.jar reader.jar
COPY start.sh start.sh
COPY docker-application.properties application.properties

ENV SERVER_PORT=8084
ENV DEBUG=false
ENV LOG_LEVEL=INFO

#WORKDIR config
ENTRYPOINT ["/bin/sh", "/app/start.sh"]