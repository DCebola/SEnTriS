FROM node:latest
WORKDIR /tests
RUN npm install -g artillery &&\
npm install faker &&\
npm install node-fetch -save &&\
npm install worker &&\
apk --no-cache add curl
ADD ./data data
ADD ./Configs/$TEST_SCENARIO.yml $TEST_SCENARIO.yml
ADD ./Configs/processor.js processor.js
CMD artillery run --output ./$TEST_SCENARIO.json ./$TEST_SCENARIO.yml > /dev/null && curl -X POST https://content.dropboxapi.com/2/files/upload --header "Authorization: Bearer $DROPBOX_TOKEN" --header "Dropbox-API-Arg: {\"path\": \"/logs/$TEST_SCENARIO.json\",\"mode\": \"add\", \"autorename\": true,\"mute\": false,\"strict_conflict\": false}" --header "Content-Type: application/octet-stream" --data-binary @$TEST_SCENARIO.json