#!/bin/bash

if [ $# -ne 1  ]; then
    echo "Usage: build&push <docker-registry>"
    exit 1
fi

cd ../Vault 
mvn clean compile package
cp ./target/Vault.war ../Deployment/Vault/Vault.war
wait

docker rm $(docker stop $(docker ps -a -q --filter="ancestor=$1/sentri-vault-api")) &> /dev/null
wait
docker rmi $(docker image ls $1/sentri-vault-api) &> /dev/null
wait

cd ../Deployment/Vault
docker build -t $1/sentri-vault-api .
wait
docker push $1/sentri-vault-api