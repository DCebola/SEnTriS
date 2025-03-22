#!/bin/bash

if [ $# -ne 2 ]; then
    echo "Usage: start <docker-registry> <dataset>"
    exit 1
fi

echo "Stopping running instances..."
for i in 1 5 10 20 0
do
    for j in $(docker ps -a --filter="ancestor=$1/fuseki-$i" --format "{{.ID}}")
    do
        docker rm $(docker stop $j) &> /dev/null
        wait
    done
done
docker run --name lubm-$2 -d -p 3030:3030 $1/fuseki-$2 