FROM maven:3.9.6-amazoncorretto-21 as builder

WORKDIR /home/maat

COPY pom.xml .
RUN mvn dependency:go-offline
RUN mvn dependency:copy-dependencies

COPY src/ src/
RUN mvn install -Dmaven.test.failure.ignore=true -Dmaven.test.skip=true

# base image to build a JRE
FROM amazoncorretto:21.0.3-alpine as corretto-jdk
#FROM openjdk:17-alpine as alpine-jdk

# required for strip-debug to work
RUN apk add --no-cache binutils

# Build small JRE image
RUN $JAVA_HOME/bin/jlink \
         --verbose \
         --add-modules ALL-MODULE-PATH \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /customjre

# main app image
FROM alpine:latest
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# copy JRE from the base image
COPY --from=corretto-jdk /customjre $JAVA_HOME
#COPY --from=alpine-jdk /customjre $JAVA_HOME


WORKDIR /home/maat

RUN apk add gettext

COPY --from=builder /home/maat/target/*.jar maat.jar
COPY docker/application.properties.template .
COPY src/main/resources/TMF638-ServiceInventory-v4.json ./schema/TMF638-ServiceInventory-v4.json
COPY src/main/resources/TMF639-ResourceInventory-v4.json ./schema/TMF639-ResourceInventory-v4.json
COPY src/main/resources/ ./src/main/resources
CMD envsubst < application.properties.template > application.properties && java -jar maat.jar
