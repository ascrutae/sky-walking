#!/bin/sh

docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
mvn clean package docker:build
docker push skywalking/skywalking-collector:latest
docker push skywalking/skywalking-collector:3.1-2017
