#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push <docker-registry>"
    exit 1
fi


docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/fuseki*")) &> /dev/null
wait
docker rmi $(docker image ls "$1/fuseki*") &> /dev/null
wait


cd ./fuseki
cp -r ../LUBM/datasets ./datasets
wait
for i in 2 4 6 8 10
do
   docker build --build-arg dataset=$i -t $1/fuseki-$i .
   wait
   docker push $1/fuseki-$i
   wait
done
rm -rf ./datasets