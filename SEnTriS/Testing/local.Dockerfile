FROM node:alpine
ARG TEST_SCENARIO=lubm-queries
ENV TEST_SCENARIO=$TEST_SCENARIO
WORKDIR /tests
RUN npm install -g npm@11.2.0 
RUN npm install faker
RUN npm install node-fetch
RUN npm install worker
RUN npm install form-data
RUN npm install -g artillery
RUN npm audit fix --force
RUN apk --no-cache add curl
ADD ./data data
ADD ./configs/$TEST_SCENARIO.yml $TEST_SCENARIO.yml
ADD ./configs/processor.js processor.js
CMD artillery run $TEST_SCENARIO.yml --output $TEST_SCENARIO.json