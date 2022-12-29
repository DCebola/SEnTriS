#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push <docker-registry>"
    exit 1
fi

cd ../Triplestore 
mvn clean compile package
cp ./target/Triplestore.war ../Deployment/Triplestore/Triplestore.war
wait

docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-triplestore-api")) &> /dev/null
wait
docker rmi $(docker image ls $1/sentri-triplestore-api) &> /dev/null
wait

cd ../Deployment/Triplestore
docker build -t $1/sentri-triplestore-api .
wait
docker push $1/sentri-triplestore-api