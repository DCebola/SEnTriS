FROM artilleryio/artillery:latest
WORKDIR /home/node/artillery/scripts
ARG TEST_SCENARIO=lubm-queries
ENV TEST_SCENARIO=$TEST_SCENARIO
ADD ./data data
ADD ./configs/$TEST_SCENARIO.yml $TEST_SCENARIO.yml
ADD ./configs/processor.js processor.js