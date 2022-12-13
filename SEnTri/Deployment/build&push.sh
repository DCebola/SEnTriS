#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: build&push <docker-registry>"
    exit 1
fi

sh ./Client/build\&push.sh $1
wait
#sh ./IAMProvider/build\&push.sh $1
#wait
#sh ./Proxy/build\&push.sh $1
#wait
#sh ./Vault/build\&push.sh $1
#wait
sh ./Triplestore/build\&push.sh $1