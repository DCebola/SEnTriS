FROM stain/jena-fuseki:latest
ARG dataset
COPY ./shiro.ini /fuseki/shiro.ini
COPY ./dataset.ttl /fuseki/configuration/
COPY ./datasets/lubm-$dataset.owl /fuseki/configuration/data/abox.owl
COPY ./datasets/lubm-ontology.owl /fuseki/configuration/data/tbox.owl