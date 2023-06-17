FROM stain/jena-fuseki:latest
ARG dataset
COPY ./dataset.ttl /fuseki/configuration/
COPY ./datasets/lubm-$dataset.owl /fuseki/configuration/data/dataset.owl