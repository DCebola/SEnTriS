#!/bin/bash
if [ $# -ne 0  ]; then
    echo "Usage: generate-truststores"
    exit 1
fi

#IAM Provider Truststore
keytool -import -file ./SSL/iam-provider-redis-cert.pem -alias redis -keystore ./SSL/iam-provider-truststore.ks
wait
keytool -import -file ./SSL/client-cert.pem -alias client -keystore ./SSL/iam-provider-truststore.ks
wait
keytool -import -file ./SSL/triplestore-cert.pem -alias triplestore -keystore ./SSL/iam-provider-truststore.ks
wait
keytool -import -file ./SSL/proxy-cert.pem -alias proxy -keystore ./SSL/iam-provider-truststore.ks
wait

#Vault Truststore
keytool -import -file ./SSL/vault-redis-cert.pem -alias redis -keystore ./SSL/vault-truststore.ks
wait
keytool -import -file ./SSL/iam-provider-cert.pem -alias iam-provider -keystore ./SSL/vault-truststore.ks
wait
keytool -import -file ./SSL/client-cert.pem -alias client -keystore ./SSL/vault-truststore.ks
wait

#Triplestore Truststore
keytool -import -file ./SSL/triplestore-redis-cert.pem -alias redis -keystore ./SSL/triplestore-truststore.ks
wait
keytool -import -file ./SSL/iam-provider-cert.pem -alias iam-provider -keystore ./SSL/triplestore-truststore.ks
wait
keytool -import -file ./SSL/proxy-cert.pem -alias proxy -keystore ./SSL/triplestore-truststore.ks
wait
keytool -import -file ./SSL/client-cert.pem -alias client -keystore ./SSL/triplestore-truststore.ks
wait

#Proxy Truststore
keytool -import -file ./SSL/proxy-redis-cert.pem -alias redis -keystore ./SSL/proxy-truststore.ks
wait
keytool -import -file ./SSL/iam-provider-cert.pem -alias iam-provider -keystore ./SSL/proxy-truststore.ks
wait
keytool -import -file ./SSL/client-cert.pem -alias client -keystore ./SSL/proxy-truststore.ks
wait
keytool -import -file ./SSL/triplestore-cert.pem -alias triplestore -keystore ./SSL/proxy-truststore.ks
wait

#Client Truststore
keytool -import -file ./SSL/iam-provider-cert.pem -alias iam-provider -keystore ./SSL/client-truststore.ks
wait
keytool -import -file ./SSL/triplestore-cert.pem -alias triplestore -keystore ./SSL/client-truststore.ks
wait
keytool -import -file ./SSL/proxy-cert.pem -alias proxy -keystore ./SSL/client-truststore.ks
wait