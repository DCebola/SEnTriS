#!/bin/bash

if [ $# -ne 2 ]; then
    echo "Usage: start <docker-registry> <dataset>"
    exit 1
fi

echo "Stoping running instances..."
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/fuseki*")) &> /dev/null
wait
docker run -d -p 3030:3030 $1/fuseki-$2