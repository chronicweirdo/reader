FROM ubuntu

RUN apt-get update
RUN apt-get install -y wget git zip unzip openjdk-17-jdk

RUN mkdir /tmp/gradle
WORKDIR /tmp/gradle
RUN wget https://services.gradle.org/distributions/gradle-7.5.1-bin.zip
RUN mkdir /opt/gradle
RUN unzip -d /opt/gradle gradle-7.5.1-bin.zip
#RUN export PATH=$PATH:/opt/gradle/gradle-7.5.1/bin/
ENV PATH="${PATH}:/opt/gradle/gradle-7.5.1/bin/"