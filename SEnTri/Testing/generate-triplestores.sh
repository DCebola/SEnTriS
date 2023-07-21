#!/bin/bash

if [ $# -ne 0  ]; then
    echo "Usage: generate-triplestores"
    exit 1
fi

for i in 1 5 10 20
do
   sh lubm-generator/generate.sh -u $i -q --consolidate Full --onto http://swat.cse.lehigh.edu/onto/univ-bench.owl
   wait
   cp Universities-1.owl ./Data/LUBM/datasets/lubm-$i.owl
   wait
done

rm Universities-1.owl
rm log.txt
