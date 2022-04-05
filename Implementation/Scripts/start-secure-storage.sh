#!/bin/bash
if [ $# -ne 2  ]; then
    echo "Usage: start-secure-storage <version> <dockerhub>" 
    exit 1
fi
version=$1
dockerhub=$2
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=${dockerhub}/secure-storage")) &> /dev/null
wait
docker run -e JAVA_OPTS="-Dsystem.version=${version}" --publish 8443:8443 -d dcebola/secure-storage