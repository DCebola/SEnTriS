#!/bin/sh

# Strip "CATALINA_OPTS=" prefix if present
CATALINA_OPTS=$(echo $CATALINA_OPTS | cut -c 11-)

# Truststore settings (password pulled from env file)
CATALINA_OPTS="$CATALINA_OPTS -Djavax.net.ssl.trustStore=${TRUSTSTORE_PATH}"
CATALINA_OPTS="$CATALINA_OPTS -Djavax.net.ssl.trustStorePassword=${TRUSTSTORE_PWD}"

# Export back
export CATALINA_OPTS