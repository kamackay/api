#!/bin/bash
yum install -y java-11-openjdk && \
  unzip -d /opt/gradle /tmp/gradle-*.zip && \
  gradle makeJar --console verbose && \
  cp build/libs/*-all-*.jar ./api.jar && \
  java -jar ./api.jar