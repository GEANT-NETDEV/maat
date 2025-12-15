FROM maven:3.9.6-amazoncorretto-21 AS builder

WORKDIR /home/maat

COPY pom.xml .
RUN mvn dependency:go-offline
RUN mvn dependency:copy-dependencies

COPY src/ src/
RUN mvn install -Dmaven.test.failure.ignore=true -Dmaven.test.skip=true

# base image to build a JRE
FROM amazoncorretto:21.0.3-alpine AS corretto-jdk
# required for strip-debug to work + openssl do pobrania łańcucha certów
RUN apk add --no-cache binutils openssl

# Build small JRE image
RUN $JAVA_HOME/bin/jlink \
         --verbose \
         --add-modules ALL-MODULE-PATH \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /customjre

RUN mkdir -p /customjre/lib/security && \
    cp -v "$JAVA_HOME/lib/security/cacerts" "/customjre/lib/security/cacerts"

COPY /docker/certs/harica-root-2015.pem /tmp/harica-root-2015.pem
COPY /docker/certs/harica-tls-rsa-root-2021.pem /tmp/harica-tls-rsa-root-2021.pem

RUN openssl s_client -servername keycloak-pionier.man.poznan.pl \
      -connect keycloak-pionier.man.poznan.pl:443 -showcerts </dev/null \
      | awk '/BEGIN CERTIFICATE/{i++} /BEGIN CERTIFICATE/,/END CERTIFICATE/{print > ("/tmp/cert-" i ".pem")}' && \
    for f in /tmp/cert-*.pem; do \
      echo "Importing $f into Java cacerts"; \
      "$JAVA_HOME/bin/keytool" -importcert -trustcacerts \
        -alias "$(basename "$f" .pem)" \
        -file "$f" \
        -keystore /customjre/lib/security/cacerts \
        -storepass changeit -noprompt || true; \
    done

# main app image
FROM alpine:latest
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# copy JRE from the base image
COPY --from=corretto-jdk /customjre $JAVA_HOME

WORKDIR /home/maat

RUN apk add --no-cache ca-certificates gettext curl openssl && update-ca-certificates

RUN mkdir -p /usr/local/share/ca-certificates && \
    openssl s_client -servername keycloak-pionier.man.poznan.pl \
      -connect keycloak-pionier.man.poznan.pl:443 -showcerts </dev/null \
      | awk '/BEGIN CERTIFICATE/{i++} /BEGIN CERTIFICATE/,/END CERTIFICATE/{print > ("/usr/local/share/ca-certificates/keycloak-" i ".crt")}' && \
    update-ca-certificates

ENV JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=/jre/lib/security/cacerts -Djavax.net.ssl.trustStorePassword=changeit"

COPY --from=builder /home/maat/target/*.jar maat.jar
COPY docker/application.properties.template .
COPY src/main/resources/TMF638-ServiceInventory-v4.json ./schema/TMF638-ServiceInventory-v4.json
COPY src/main/resources/TMF639-ResourceInventory-v4.json ./schema/TMF639-ResourceInventory-v4.json
COPY src/main/resources/ ./src/main/resources
CMD envsubst < application.properties.template > application.properties && java -jar maat.jar
