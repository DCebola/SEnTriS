FROM node:alpine
WORKDIR /tests
RUN npm install -g npm@11.2.0 
RUN npm install @faker-js/faker
RUN npm install node-fetch
RUN npm install worker
RUN npm install form-data
RUN npm install -g artillery
RUN npm audit fix --force
RUN apk --no-cache add curl
ADD ./data data
ADD ./configs/lubm-query.yml lubm-query.yml
ADD ./configs/lubm-upload.yml lubm-upload.yml
ADD ./configs/lubm-debug.yml lubm-debug.yml
ADD ./configs/processor.js processor.js
#ENV DEBUG=http:request,http:response
ENTRYPOINT ["artillery", "run"]