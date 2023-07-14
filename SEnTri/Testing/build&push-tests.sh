#!/bin/bash

if [ $# -ne 2  ]; then
    echo "Usage: build&push-tests <docker-registry> <dropbox-token>"
    exit 1
fi

docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/test")) &> /dev/null

scenarios=("upload-2", "upload-4", "upload-6", "upload-8", "upload-10", "query-2", "query-4", "query-6", "query-8", "query-10")
for scenario in ${scenarios[@]}; do
    docker build -t $1/sentri-test-$scenario \
    --build-arg TEST_SCENARIO=$scenario  \
    --build-arg DROPBOX_TOKEN=$dropbox_token  \
    .
    wait
    docker push $1/sentri-test\
done

