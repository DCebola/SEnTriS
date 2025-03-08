#!/bin/bash

if [ $# -ne 2 ]; then
    echo "Usage: start <docker-registry> <dataset>"
    exit 1
fi

echo "Stopping running instances..."
for i in $(docker container ps -a --filter="ancestor=$1/fuseki-$2" --format "{{.ID}}")
do
    docker rm $(docker stop $i) &> /dev/null
    wait
done
docker run --name lubm-$2 -d -p 3030:3030 $1/fuseki-$2 