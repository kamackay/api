FROM alpine:latest
WORKDIR /api

RUN apk upgrade --update --no-cache && \
    apk add --update --no-cache \
        openjdk11 \
        maven \
        bash

COPY pom.xml /api/
RUN mvn dependency:go-offline

COPY ./src /api/src
COPY ./creds.json /api/

RUN mvn package && \
    cp target/*jar-with-dependencies.jar ./api.jar && \
    rm -rf ~/.m2 && \
    rm -rf ./src && \
    rm -rf ./target

RUN apk del --no-cache maven

ENV CREDENTIALS_FILE /api/creds.json
CMD [ "java", "-jar", "-Xmx400m", "-Xss4m", "api.jar" ]
