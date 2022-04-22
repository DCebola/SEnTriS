#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push-secure-storage <docker-registry>"
    exit 1
fi

cd ../Secure-Storage 
mvn clean compile package
cp ./target/Secure-Storage.war ../Scripts/API/Secure-Storage.war
wait
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/secure-storage")) &> /dev/null
wait
docker rmi $(docker image ls $1/secure-storage) &> /dev/null
wait

cd ../Scripts/API
docker build -t $1/secure-storage .
wait
docker push $1/secure-storage
wait