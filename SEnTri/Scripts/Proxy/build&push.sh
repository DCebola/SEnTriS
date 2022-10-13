#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push <docker-registry>"
    exit 1
fi

cd ../Proxy 
mvn clean compile package
cp ./target/Proxy.war ../Scripts/Proxy/Proxy.war
wait

docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/SEnTri/proxy")) &> /dev/null
wait
docker rmi $(docker image ls $1/SEnTri/proxy) &> /dev/null
wait

cd ../Scripts/Proxy
docker build -t $1/SEnTri/proxy .
wait
docker push $1/SEnTri/proxy