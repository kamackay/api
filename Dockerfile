FROM alpine:latest
WORKDIR /home/api

RUN apk upgrade --update --no-cache && \
    apk add --update --no-cache \
        openjdk11 \
        maven \
        bash

ADD . .

RUN mvn install && \
    cp target/*jar-with-dependencies.jar ./api.jar && \
    rm -rf ~/.m2 && \
    rm -rf ./src && \
    rm -rf ./target

RUN apk del --no-cache maven

ENV CREDENTIALS_FILE /home/api/creds.json
CMD ["java", "-jar", "api.jar"]
