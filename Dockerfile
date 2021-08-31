FROM gradle:jdk15 as builder

WORKDIR /api

COPY build.gradle ./

RUN gradle getDeps

COPY ./ ./

RUN gradle makeJar --console verbose && \
    cp build/libs/*-all-*.jar ./api.jar

FROM registry.access.redhat.com/ubi8:latest as platform

RUN yum update -y && yum install -y java-11-openjdk

WORKDIR /api/

FROM scratch

COPY --from=platform / /

COPY --from=builder /api/api.jar ./

ENV CREDENTIALS_FILE /api/creds.json
ENV SECRETS_FILE /api/secrets.json
ENV CONFIG_FILE /api/config.json

CMD [ "java", "-jar", "api.jar" ]
