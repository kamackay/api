FROM maven:3.6.2-jdk-12 as builder

WORKDIR /api

COPY pom.xml .

RUN mvn dependency:go-offline

COPY ./src ./src

RUN mvn install && \
    cp target/*jar-with-dependencies.jar ./api.jar

FROM openjdk:12-alpine

WORKDIR /api/

COPY --from=builder /api .

ENV CREDENTIALS_FILE /api/creds.json

CMD [ "java", "-jar", "-Xmx400m", "-Xss200m", "api.jar" ]
