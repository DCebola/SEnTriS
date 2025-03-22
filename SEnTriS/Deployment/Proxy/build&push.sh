#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push <docker-registry>"
    exit 1
fi

cd ../Proxy 
mvn clean compile package
cp ./target/Proxy.war ../Deployment/Proxy/Proxy.war
wait

for i in $(docker ps -a --filter="ancestor=$1/sentris-proxy-api" --format "{{.ID}}")
do
    docker rm $(docker stop $i) &> /dev/null
    wait
done
for i in $(docker image ls "$1/sentris-proxy-api" --format "{{.ID}}")
do
    docker rmi $i &> /dev/null
    wait
done

cd ../Deployment/Proxy
docker build -t $1/sentris-proxy-api .
wait
docker push $1/sentris-proxy-api