#!/bin/bash

if [ $# -ne 2  ]; then
    echo "Usage: build&push-tests <docker-registry> <dropbox-token>"
    exit 1
fi
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-experimental-evaluation")) &> /dev/null
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-concurrency-test")) &> /dev/null
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-write-test")) &> /dev/null
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-stress-test")) &> /dev/null
wait
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-experimental-evaluation")) &> /dev/null
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-concurrency-test")) &> /dev/null
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-write-test")) &> /dev/null
docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-stress-test")) &> /dev/null
wait

scenarios=("experimental-evaluation", "concurrency-test", "write-test", "stress-test")
for scenario in ${scenarios[@]}; do
    docker build -t $1/sentri-$scenario \
    --build-arg TEST_SCENARIO=$scenario  \
    --build-arg DROPBOX_TOKEN=$dropbox_token  \
    .
    wait
    docker push $1/sentri-$scenario
done

