#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push-tests <docker-registry>"
    exit 1
fi

for scenario in lubm
do
    docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentris-test-$scenario" --format "{{.ID}}")) &> /dev/null
    wait
    docker build -f Dockerfile \
    -t $1/sentris-test-$scenario \
    --build-arg TEST_SCENARIO=$scenario \
    .
    wait
    docker push $1/sentris-$scenario
done