#!/bin/bash

if [ $# -ne 0 ]; then
    echo "Usage: reset"
    exit 1
fi

echo "Resetting system..."
docker rm $(docker stop $(docker ps -q -f "name=sentri")) &> /dev/null
wait
docker network rm $(docker network ls -q -f 'name=sentri')
wait