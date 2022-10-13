#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: start <docker-registry>"
    exit 1
fi

docker network rm $(docker network ls -q -f 'name=triplestore-backend')
wait
docker network rm $(docker network ls -q -f 'name=triplestore-frontend')
wait
docker network rm $(docker network ls -q -f 'name=proxy-backend')
wait
docker network rm $(docker network ls -q -f 'name=proxy-frontend')
wait
export var DOCKER_REGISTRY=$1
wait
docker-compose up --force-recreate --remove-orphans --detach
wait
unset DOCKER_REGISTRY
