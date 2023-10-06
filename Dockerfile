FROM gradle:jdk17 as builder

WORKDIR /api

COPY build.gradle ./

RUN gradle getDeps

COPY ./ ./

RUN gradle makeJar --console verbose && \
    cp build/libs/*-all-*.jar ./api.jar

FROM alpine:latest as platform

RUN apk update --no-cache && apk upgrade --no-cache && apk add --no-cache openjdk17

WORKDIR /api/

FROM scratch as penultimate

COPY --from=platform / /

COPY --from=builder /api/api.jar ./

ENV CREDENTIALS_FILE /api/creds.json
ENV SECRETS_FILE /api/secrets.json
ENV CONFIG_FILE /api/config.json

CMD [ "java", "-jar", "api.jar" ]
