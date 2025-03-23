#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push <docker-registry>"
    exit 1
fi

for i in 0 1 5 10 20
do
    for j in $(docker ps -a --filter="ancestor=$1/fuseki-$i" --format "{{.ID}}")
    do
        docker rm $(docker stop $j) &> /dev/null
        wait
    done
done
for j in $(docker image ls "$1/fuseki*" --format "{{.ID}}")
do
    docker rmi $j &> /dev/null
    wait
done

cd ./fuseki
cp -r ../data/datasets ./datasets
wait
cp ../data/ontologies/lubm-ontology.owl ./datasets
wait
for i in 0 1 2 3 4 5 10 20
do
   docker build --build-arg dataset=$i -t $1/fuseki-$i .
   wait
   docker push $1/fuseki-$i
   wait
done
rm -rf ./datasets