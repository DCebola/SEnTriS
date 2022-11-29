#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push <docker-registry>"
    exit 1
fi

cd ../IAMProvider 
mvn clean compile package
cp ./target/IAMProvider.war ../Scripts/IAMProvider/IAMProvider.war
wait

docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-iam-provider-api")) &> /dev/null
wait
docker rmi $(docker image ls $1/sentri-iam-provider-api) &> /dev/null
wait

cd ../Scripts/IAMProvider
docker build -t $1/sentri-iam-provider-api .
wait
docker push $1/sentri-iam-provider-api