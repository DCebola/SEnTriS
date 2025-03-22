#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push <docker-registry>"
    exit 1
fi

cd ../Vault 
mvn clean compile package
cp ./target/Vault.war ../Deployment/Vault/Vault.war
wait

for i in $(docker ps -a --filter="ancestor=$1/sentris-vault-api" --format "{{.ID}}")
do
    docker rm $(docker stop $i) &> /dev/null
    wait
done
for i in $(docker image ls "$1/sentris-vault-api" --format "{{.ID}}")
do
    docker rmi $i &> /dev/null
    wait
done

cd ../Deployment/Vault
docker build -t $1/sentris-vault-api .
wait
docker push $1/sentris-vault-api