#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push <docker-registry>"
    exit 1
fi

cd ../Client 
mvn clean compile package
cp ./target/Client.war ../Deployment/Client/Client.war
wait

for i in $(docker ps -a --filter="ancestor=$1/sentri-client-api" --format "{{.ID}}")
do
    docker rm $(docker stop $i) &> /dev/null
    wait
done
for i in $(docker image ls "$1/sentri-client-api" --format "{{.ID}}")
do
    docker rmi $i &> /dev/null
    wait
done

cd ../Deployment/Client
docker buildx build -t $1/sentri-client-api .
wait
docker push $1/sentri-client-api
