#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: start <docker-registry>"
    exit 1
fi

echo "Resetting system..."
for i in $(docker ps -q -f "name=sentri" --format "{{.ID}}")
do
    docker rm $(docker stop $i) &> /dev/null
    wait
done
docker network rm $(docker network ls -q -f 'name=sentri' --format "{{.ID}}")
wait
export var DOCKER_REGISTRY=$1
cd ./IAMProvider 
docker-compose up --force-recreate --remove-orphans --detach
wait
cd ../Vault 
docker-compose up --force-recreate --remove-orphans --detach
wait
cd ../Proxy 
docker-compose up --force-recreate --remove-orphans --detach
wait
cd ../Triplestore 
docker-compose up --force-recreate --remove-orphans --detach
wait
cd ../Client 
docker-compose up --force-recreate --remove-orphans --detach
wait
unset DOCKER_REGISTRY