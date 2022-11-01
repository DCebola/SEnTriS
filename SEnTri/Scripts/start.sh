#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: start <docker-registry>"
    exit 1
fi

echo "Resetting system..."
docker rm $(docker stop $(docker ps -q -f "name=sentri")) &> /dev/null
wait
#docker network rm $(docker network ls -q -f 'name=sentri')
#wait
export var DOCKER_REGISTRY=$1
wait
cd ./Triplestore 
docker-compose up --force-recreate --remove-orphans --detach
#cd ../Proxy 
#docker-compose up --force-recreate --remove-orphans --detach
#cd ../DataOwner 
#docker-compose up --force-recreate --remove-orphans --detach
#wait
unset DOCKER_REGISTRY
