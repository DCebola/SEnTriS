#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push <docker-registry>"
    exit 1
fi

cd ../DataOwner 
mvn clean compile package
cp ./target/DataOwner.war ../Scripts/DataOwner/DataOwner.war
wait

docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-data-owner")) &> /dev/null
wait
docker rmi $(docker image ls $1/sentri-data-owner) &> /dev/null
wait

cd ../Scripts/DataOwner
docker build -t $1/sentri-data-owner .
wait
docker push $1/sentri-data-owner
