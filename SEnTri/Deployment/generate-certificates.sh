#!/bin/bash
if [ $# -ne 0  ]; then
    echo "Usage: generate-certificates"
    exit 1
fi

rm SSL/*

generate_cert() {
    local name=$1
    local subjectName=$2

    local keyfile=./SSL/${name}-key.pem
    local certfile=./SSL/${name}-cert.pem
    [ -f $keyfile ] || touch $keyfile && openssl genrsa -out $keyfile 4096
    openssl req \
        -new -sha256 \
        -subj "/C=PT/ST=Setubal/L=Almada/O=NOVA.ID.FCT/OU=DI/CN=$subjectName" \
        -key $keyfile | \
        openssl x509 \
            -req -sha256 \
            -CA ./SSL/ca-cert.pem \
            -CAkey ./SSL/ca-key.pem \
            -CAserial ./SSL/ca-serial.txt \
            -CAcreateserial \
            -days 365 \
            -out $certfile
}

openssl req -x509 -newkey rsa:4096 -keyout ./SSL/ca-key.pem -out ./SSL/ca-cert.pem -days 365 -subj "/C=PT/ST=Setubal/L=Almada/O=NOVA.ID.FCT/OU=DI/CN=dcebola"
wait
#IAM Provider certificates
generate_cert iam-provider sentri-iam-provider-api
wait
generate_cert iam-provider-redis sentri-iam-provider-db
wait
[ -f ./SSL/iam-provider-redis.dh ] || openssl dhparam -out ./SSL/iam-provider-redis.dh 2048
wait

#Vault certificates
generate_cert vault sentri-vault-api 
wait
generate_cert vault-redis sentri-vault-db
wait
[ -f ./SSL/vault-redis.dh ] || openssl dhparam -out ./SSL/vault-redis.dh 2048
wait

#Proxy certificates
generate_cert proxy sentri-proxy
wait
generate_cert proxy-redis sentri-proxy-db
wait
[ -f ./SSL/proxy-redis.dh ] || openssl dhparam -out ./SSL/proxy-redis.dh 2048
wait

#Triplestore certificates
generate_cert triplestore sentri-triplestore-api
wait
generate_cert triplestore-redis sentri-triplestore-db
wait
[ -f ./SSL/triplestore-redis.dh ] || openssl dhparam -out ./SSL/triplestore-redis.dh 2048
wait

#Client certificate
generate_cert client sentri-client-api