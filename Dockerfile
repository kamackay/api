FROM kamackay/alpine

RUN mkdir /api/
WORKDIR /api/
ADD . .

RUN yarn build && \
    rm -rf ./src && \
    rm yarn.lock

EXPOSE 9876

CMD ["yarn", "start"]