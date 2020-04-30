FROM maven:3.6.2-jdk-12 as builder

WORKDIR /api

COPY pom.xml .

RUN mvn dependency:go-offline

COPY ./src ./src

RUN mvn install && \
    cp target/*jar-with-dependencies.jar ./api.jar

FROM registry.access.redhat.com/ubi8:latest

RUN yum install -y java-11-openjdk

WORKDIR /api/

COPY --from=builder /api/api.jar ./

ENV CREDENTIALS_FILE /api/creds.json

CMD [ "java", "-jar", "-Xmx400m", "-Xss200m", "api.jar" ]
