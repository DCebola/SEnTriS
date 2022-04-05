#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push-secure-storage <dockerhub>"
    exit 1
fi

dockerhub=$1
cp ./server.xml ../Secure-Storage
cp -r ./ssl ../Secure-Storage

cd ../Secure-Storage 
mvn clean compile package
wait
echo "-----------------------------------------------------[FINISHED PACKAGING]"
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=${dockerhub}/secure-storage")) &> /dev/null
wait
docker rmi $(docker image ls $dockerhub/secure-storage)&> /dev/null
wait
docker build -t $dockerhub/secure-storage .
wait
echo "--------------------------------------------------------[BUILT NEW IMAGE]"

docker push $dockerhub/secure-storage
wait
echo "-------------------------------------------------------[PUSHED NEW IMAGE]"

rm ./server.xml
rm -r ./ssl


