#!/bin/bash
if [ $# -ne 0  ]; then
    echo "Usage: generate-x509-certificate"
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
generate_cert triplestore sentri-triplestore-api
wait
generate_cert proxy sentri-proxy
wait
generate_cert data-owner sentri-data-owner
wait
generate_cert redis sentri-triplestore-db
wait

[ -f ./SSL/redis.dh ] || openssl dhparam -out ./SSL/redis.dh 2048

keytool -import -file ./SSL/redis-cert.pem -alias redis -keystore ./SSL/triplestore-truststore.ks
wait
keytool -import -file ./SSL/proxy-cert.pem -alias proxy -keystore ./SSL/triplestore-truststore.ks
wait
keytool -import -file ./SSL/data-owner-cert.pem -alias data-owner -keystore ./SSL/triplestore-truststore.ks
wait
keytool -import -file ./SSL/ca-cert.pem -alias ca -keystore ./SSL/triplestore-truststore.ks
wait

keytool -import -file ./SSL/triplestore-cert.pem -alias triplestore -keystore ./SSL/proxy-truststore.ks
wait
keytool -import -file ./SSL/ca-cert.pem -alias ca -keystore ./SSL/proxy-truststore.ks
wait

keytool -import -file ./SSL/triplestore-cert.pem -alias triplestore -keystore ./SSL/data-owner-truststore.ks
wait
keytool -import -file ./SSL/ca-cert.pem -alias ca -keystore ./SSL/data-owner-truststore.ks