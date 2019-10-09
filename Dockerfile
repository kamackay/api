FROM alpine:latest
WORKDIR /api/
ADD . .

RUN apk upgrade --update --no-cache ; apk add --no-cache \
    nodejs \
    yarn

RUN yarn global add typescript && \
    yarn build && \
    yarn global remove typescript && \
    rm yarn.lock && \
    yarn cache clean && \
    rm -rf ./src

EXPOSE 9876

CMD ["yarn", "start"]
