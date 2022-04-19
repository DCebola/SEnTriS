#!/bin/bash
if [ $# -ne 1  ]; then
    echo "Usage: generate-x509-certificate <password>"
    exit 1
fi

if [ -f "./SSL/cert.pem" ]; then
    sed -i '$ d' ./Secrets/api-secrets.env
fi
password=$1
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -passout pass:$password -subj "/C=PT/ST=Setubal/L=Almada/O=NOVA.ID.FCT/OU=DI/CN=dcebola"
echo "SERVER_CERT_PWD=$password" >> ./Secrets/api-secrets.env
mv *.pem ./SSL