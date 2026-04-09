FROM maven:3.9.6-amazoncorretto-21 AS builder

WORKDIR /home/maat

COPY pom.xml .
RUN mvn dependency:go-offline
RUN mvn dependency:copy-dependencies

COPY src/ src/
RUN mvn install -Dmaven.test.failure.ignore=true -Dmaven.test.skip=true

# base image to build a JRE
FROM amazoncorretto:21.0.3-alpine AS corretto-jdk
ARG TLS_CERT_HOST=""
ARG TLS_CERT_PORT="443"

# required for strip-debug to work and for downloading certificate chains
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

COPY docker/certs/ /tmp/certs/

RUN find /tmp/certs -type f \( -name '*.crt' -o -name '*.pem' \) | while read -r cert; do \
      echo "Importing $cert into Java cacerts"; \
      normalized="/tmp/$(basename "$cert").der"; \
      tr -d '\r' < "$cert" > /tmp/import-cert.pem && \
      openssl x509 -inform PEM -in /tmp/import-cert.pem -outform DER -out "$normalized" && \
      "$JAVA_HOME/bin/keytool" -importcert -trustcacerts \
        -alias "$(basename "$cert" | sed 's/[^A-Za-z0-9._-]/-/g')" \
        -file "$normalized" \
        -keystore /customjre/lib/security/cacerts \
        -storepass changeit -noprompt || echo "Skipping Java cacerts import for $cert"; \
    done

RUN if [ -n "$TLS_CERT_HOST" ]; then \
      openssl s_client -servername "$TLS_CERT_HOST" \
        -connect "$TLS_CERT_HOST:$TLS_CERT_PORT" -showcerts </dev/null \
        | awk '/BEGIN CERTIFICATE/{i++} /BEGIN CERTIFICATE/,/END CERTIFICATE/{print > ("/tmp/remote-cert-" i ".pem")}'; \
      for cert in /tmp/remote-cert-*.pem; do \
        if [ -f "$cert" ]; then \
          echo "Importing $cert from $TLS_CERT_HOST into Java cacerts"; \
          normalized="/tmp/$(basename "$cert").der"; \
          tr -d '\r' < "$cert" > /tmp/import-cert.pem; \
          openssl x509 -inform PEM -in /tmp/import-cert.pem -outform DER -out "$normalized"; \
          "$JAVA_HOME/bin/keytool" -importcert -trustcacerts \
            -alias "$(basename "$cert" .pem)" \
            -file "$normalized" \
            -keystore /customjre/lib/security/cacerts \
            -storepass changeit -noprompt; \
        fi; \
      done; \
    fi

# main app image
FROM alpine:latest
ARG TLS_CERT_HOST=""
ARG TLS_CERT_PORT="443"
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# copy JRE from the base image
COPY --from=corretto-jdk /customjre $JAVA_HOME

WORKDIR /home/maat

RUN apk add --no-cache ca-certificates gettext curl openssl && update-ca-certificates

COPY docker/certs/ /usr/local/share/ca-certificates/custom/

RUN find /usr/local/share/ca-certificates/custom -type f \( -name '*.crt' -o -name '*.pem' \) | while read -r cert; do \
      name="$(basename "$cert")"; \
      name="${name%.pem}"; \
      name="${name%.crt}"; \
      cp "$cert" "/usr/local/share/ca-certificates/${name}.crt"; \
    done && \
    if [ -n "$TLS_CERT_HOST" ]; then \
      openssl s_client -servername "$TLS_CERT_HOST" \
        -connect "$TLS_CERT_HOST:$TLS_CERT_PORT" -showcerts </dev/null \
        | awk '/BEGIN CERTIFICATE/{i++} /BEGIN CERTIFICATE/,/END CERTIFICATE/{print > ("/usr/local/share/ca-certificates/remote-cert-" i ".crt")}'; \
    fi && \
    update-ca-certificates

ENV JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=/jre/lib/security/cacerts -Djavax.net.ssl.trustStorePassword=changeit"

COPY --from=builder /home/maat/target/*.jar maat.jar
COPY docker/application.properties.template .
COPY src/main/resources/TMF638-ServiceInventory-v4.json ./schema/TMF638-ServiceInventory-v4.json
COPY src/main/resources/TMF639-ResourceInventory-v4.json ./schema/TMF639-ResourceInventory-v4.json
COPY src/main/resources/ ./src/main/resources
CMD envsubst < application.properties.template > application.properties && java -jar maat.jar
