#!/bin/bash

if [ $# -ne 2  ]; then
    echo "Usage: run-tests <docker-registry> <test-name>"
    exit 1
fi

for scenario in lubm-upload
do
    docker run --rm --add-host=host.docker.internal:host-gateway -v $PWD/results/$2:/tests/lubm-upload.json $1/sentris-test-$scenario
done

