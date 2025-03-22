FROM stain/jena-fuseki:latest
ARG dataset
COPY ./shiro.ini /fuseki/shiro.ini
COPY ./configs/lubm-$dataset.ttl /fuseki/configuration/dataset.ttl
COPY ./datasets/lubm-$dataset.owl /fuseki/configuration/data/abox.owl
COPY ./datasets/lubm-ontology.owl /fuseki/configuration/data/tbox.owl