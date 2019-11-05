FROM maven:3.6.2-jdk-12 as builder

WORKDIR /api

COPY pom.xml /api/

RUN mvn package && \
    mvn dependency:copy-dependencies

COPY ./src /api/src
COPY ./creds.json /api/

RUN mvn package && \
    cp target/*jar-with-dependencies.jar ./api.jar && \
    rm -rf ./target && \
    rm -rf ./src && \
    rm pom.xml

FROM openjdk:12-alpine

WORKDIR /api/

COPY --from=builder /api .

ENV CREDENTIALS_FILE /api/creds.json
CMD [ "java", "-jar", "-Xmx400m", "-Xss4m", "api.jar" ]
