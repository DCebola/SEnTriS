#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push-lubm-test-runner <docker-registry>"
    exit 1
fi

docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/lubm-test-runner" --format "{{.ID}}")) &> /dev/null
wait
docker build -f Dockerfile -t $1/lubm-test-runner .
wait
docker push $1/lubm-test-runner