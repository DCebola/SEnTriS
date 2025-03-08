#!/bin/bash

if [ $# -ne 0 ]; then
    echo "Usage: reset"
    exit 1
fi

echo "Resetting system..."
for i in $(docker ps -q -f "name=sentri" --format "{{.ID}}")
do
    docker rm $(docker stop $i) &> /dev/null
    wait
done
docker network rm $(docker network ls -q -f 'name=sentri' --format "{{.ID}}")
wait