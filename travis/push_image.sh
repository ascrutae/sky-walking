#!/bin/sh


push_image() {
  docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
  mvn clean package docker:build
  docker push skywalking/skywalking-collector:latest
  docker push skywalking/skywalking-collector:3.0.1-2017
}


push_image
echo "Push is Done!"
