#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: start-secure-storage <docker-registry>"
    exit 1
fi

docker network rm $(docker network ls -q -f 'name=secure-storage-backend-network')
wait
export var DOCKER_REGISTRY=$1
wait
docker-compose up --force-recreate --remove-orphans
wait
unset DOCKER_REGISTRY
