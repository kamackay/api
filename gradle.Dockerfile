FROM gradle:jdk11

WORKDIR /api

COPY ./ ./

ENV CREDENTIALS_FILE /api/creds.json
ENV SECRETS_FILE /api/secrets.json
ENV CONFIG_FILE /api/config.json

CMD gradle run