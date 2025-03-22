#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: stop <docker-registry>"
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