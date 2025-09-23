#!/bin/bash

if [ $# -ne 2 ]; then
    echo "Usage: lubm-benchmark <docker-registry> <experiment-id>"
    exit 1
fi

REGISTRY=$1
EXPERIMENT_ID=$2
STARTUP_WAIT=15   # seconds to wait after start.sh (adjust as needed)

run_scenario() {
    local dataset=$1
    local version=$2

    if [ "$(curl -k -s -o /dev/null -w "%{http_code}" -X POST https://localhost:8091/IAMProvider/api/ctrl/init)" = "200" ]; then
          for scenario in lubm-query; do
              echo " > $scenario (dataset=$dataset, version=$version)"
              docker run --rm \
                  --add-host=host.docker.internal:host-gateway \
                  -v "$PWD/results/$EXPERIMENT_ID:/tests/$scenario.json" \
                  "$REGISTRY/lubm-test-runner" \
                  --insecure \
                  --variables "{ \"dataset\": [\"$dataset\"], \"version\": [\"$version\"] }" \
                  "$scenario.yml" \
                  --output "$scenario.json" \
                  > "tmp/$version-$dataset-$scenario.txt"
              wait
          done
    else
        echo "Init API did not return 200, skipping dataset $dataset"
    fi
}

# Ensure tmp and results directories exist
mkdir -p tmp "results/$EXPERIMENT_ID"

# Loop through datasets
for i in lubm-2 lubm-3 lubm-4 lubm-5; do
  for v in v1 v2; do
    echo "Restarting deployment for $i, $v..."
    cd ../Deployment
    sh start.sh "$REGISTRY"
    cd ../Testing
    echo "Waiting $STARTUP_WAIT seconds for deployment to stabilize..."
    sleep $STARTUP_WAIT
    echo "Running benchmark for $i, $v..."
    run_scenario "$i" "$v"
  done
done

