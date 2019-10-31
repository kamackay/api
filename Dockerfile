FROM openjdk:12-alpine as builder
WORKDIR /api

RUN apk upgrade --update --no-cache && \
    apk add --update --no-cache \
        maven

COPY pom.xml /api/
RUN mvn dependency:go-offline

COPY ./src /api/src
COPY ./creds.json /api/

RUN mvn package && \
    cp target/*jar-with-dependencies.jar ./api.jar && \
    rm -rf ./target && \
    rm -rf ./src && \
    rm pom.xml

FROM openjdk:12

WORKDIR /api/

COPY --from=builder /api .

ENV CREDENTIALS_FILE /api/creds.json
CMD [ "java", "-jar", "-Xmx400m", "-Xss4m", "api.jar" ]
