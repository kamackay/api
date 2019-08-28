FROM alpine:latest

RUN mkdir /home/api && mkdir /db/ && \
    apk add --update --no-cache \
        openjdk11 \
        maven \
        bash

WORKDIR /home/api

ADD . .

RUN rm -rf ./db

ADD ./db /db/

RUN mvn clean install && \
    cp target/api.jar ./api.jar && \
    rm -rf ./src \
    rm -rf ./target

CMD ["java", "-jar", "api.jar"]
