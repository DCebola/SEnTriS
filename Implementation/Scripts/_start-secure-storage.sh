#!/bin/bash
if [ $# -ne 1  ]; then
    echo "Usage: start-secure-storage <dockerhub>" 
    exit 1
fi
dockerhub=$1
docker-compose stop
wait
docker-compose rm
wait
docker network create my-net
#docker run -e JAVA_OPTS="-Dsystem.version=${version}" --publish 8443:8443 -d dcebola/secure-storage
DOCKERHUB=${dockerhub} docker-compose up