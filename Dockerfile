FROM kamackay/alpine

RUN mkdir /home/api
WORKDIR /home/api

ADD . .

RUN mvn clean install && ls target

CMD ["java", "-jar", "api.jar"]
