FROM alpine:latest

RUN mkdir /home/api && \
        apk upgrade --update --no-cache && \
        apk add --update --no-cache \
        openjdk11 \
        maven \
        bash

WORKDIR /home/api

ADD . .

RUN mvn clean install && \
        cp target/api.jar ./api.jar && \
        rm -rf ./src \
        rm -rf ./target && \
        rm -rf /tmp && \
        apk del maven && \
        rm -rf ~/.m2

CMD ["java", "-jar", "api.jar"]
