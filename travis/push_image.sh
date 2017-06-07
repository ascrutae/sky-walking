#!/bin/sh

IMAGE_VERSION=3.0.3-2017
docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
mvn clean package docker:build
docker push skywalking/skywalking-collector:latest
docker push skywalking/skywalking-collector:${IMAGE_VERSION}
