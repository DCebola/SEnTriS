#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push <docker-registry>"
    exit 1
fi

cd ../Client 
mvn clean compile package
cp ./target/Client.war ../Scripts/Client/Client.war
wait

docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-client")) &> /dev/null
wait
docker rmi $(docker image ls $1/sentri-client) &> /dev/null
wait

cd ../Scripts/Client
docker build -t $1/sentri-client .
wait
docker push $1/sentri-client
