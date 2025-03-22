#!/bin/bash

if [ $# -ne 0 ]; then
    echo "Usage: reset"
    exit 1
fi

echo "Resetting system..."
for i in $(docker ps -a -f "name=sentris" --format "{{.ID}}")
do
    docker rm $(docker stop $i) &> /dev/null
    wait
done
docker network rm $(docker network ls -f 'name=sentris' --format "{{.ID}}")
wait