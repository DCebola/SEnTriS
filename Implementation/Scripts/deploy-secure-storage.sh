#!/bin/bash

if [ $# -ne 2  ]; then
    echo "Usage: push-secure-storage <version> <dockerhub>"
    exit 1
fi

version=$1
dockerhub=$2
cp ./server.xml ../Secure-Storage
cp -r ./ssl ../Secure-Storage

cd ../Secure-Storage 
mvn -Dversion=$version clean compile package
wait
echo "-----------------------------------------------------[FINISHED PACKAGING]"

docker rmi $dockerhub/secure-storage
wait
echo "------------------------------------------------------[DELETED OLD IMAGE]"

docker build --build-arg VERSION=${version} -t $dockerhub/secure-storage .
wait
echo "--------------------------------------------------------[BUILT NEW IMAGE]"

docker push $dockerhub/secure-storage
wait
echo "-------------------------------------------------------[PUSHED NEW IMAGE]"

rm ./server.xml
rm -r ./ssl


