#!/bin/bash

if [ $# -ne 2  ]; then
    echo "Usage: lubm-debug <docker-registry> <experiment-id>"
    exit 1
fi

if [ "$(curl -k -s -o /dev/null -w "%{http_code}" -X POST https://localhost:8091/IAMProvider/api/ctrl/init)" = "200" ]; then
  for dataset in lubm-1
  do
    for version in v2
    do
      for scenario in lubm-debug
      do
        echo " > $scenario (dataset=$dataset, version=$version)"
        docker run --rm \
          --add-host=host.docker.internal:host-gateway \
          -v "$PWD/results/$2:/tests/$scenario.json" \
          "$1/lubm-test-runner" \
          --insecure \
          --variables "{ \"triplestoreID\": [\"test-$version-$dataset\"], \"version\": [\"$version\"], \"dataset\": [\"$dataset\"], \"username\": [\"admin\"], \"password\": [\"admin\"] }" \
          "$scenario.yml" \
          --output "$scenario.json" \
          > "tmp/$version-$dataset-$scenario.txt"
        wait
      done
    done
  done
fi
