FROM gradle:latest as BUILD

COPY . /source

WORKDIR /source
RUN gradle build

FROM openjdk:latest as RUN

RUN mkdir /db
WORKDIR app
COPY --from=BUILD /source/build/libs/reader.jar reader.jar
COPY start.sh start.sh
COPY docker-application.properties application.properties

ENV SERVER_PORT=8084
ENV DEBUG=false
ENV LOG_LEVEL=INFO
ENV ENABLE_FOLDER_WATCHING=true
ENV JAVA_OPTS="-Xms128m -Xmx256m"
ENV VERIFY_ON_INITIAL_SCAN=true
ENV SERVER_TOMCAT_MAX_THREADS=20
ENV SCHEDULING_ENABLED=true
ENV RESCAN_CRON="0 */1 * * * *"

#WORKDIR config
ENTRYPOINT ["/bin/sh", "/app/start.sh"]