#!/bin/bash

if [ $# -ne 2  ]; then
    echo "Usage: build&push-tests <docker-registry> <dropbox-token>"
    exit 1
fi
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-upload" --format "{{.ID}}")) &> /dev/null
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-lubm" --format "{{.ID}}")) &> /dev/null
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-lubm-extra" --format "{{.ID}}")) &> /dev/null
wait

scenarios=("upload", "lubm", "lubm-extra")
for scenario in ${scenarios[@]}; do
    docker build -t $1/sentri-$scenario \
    --build-arg TEST_SCENARIO=$scenario \
    --build-arg DROPBOX_TOKEN=$2 \
    .
    wait
    docker push $1/sentri-$scenario
done

