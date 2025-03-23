#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: run-tests <docker-registry>"
    exit 1
fi

for scenario in lubm-upload
do
    docker run --rm --add-host=host.docker.internal:host-gateway $1/sentris-test-$scenario
done

