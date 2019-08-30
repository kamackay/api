FROM alpine:latest

RUN mkdir /home/api && \
    apk add --update --no-cache \
        openjdk11 \
        maven \
        bash

WORKDIR /home/api

ADD . .

RUN mvn clean install && \
    cp target/api.jar ./api.jar && \
    rm -rf ./src \
    rm -rf ./target

EXPOSE 5000

CMD ["java", "-jar", "api.jar"]
