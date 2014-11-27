#!/bin/bash

TEST_PROPS=testmodel/testmodel.properties

for dep in keytool; do
  if test -z $(which $dep); then
    echo "ERROR: $dep not found - cannot continue"
    exit 1
  fi
done

# Create a java keystore with a generated key-pair
echo '#----> Creating keystore'
keytool -genkey -noprompt \
    -keysize 2048 \
    -alias SELF \
    -keyalg RSA \
    -dname "CN=intermine-ci, C=GB" \
    -storepass intermine \
    -keypass intermine \
    -keystore testmodel/webapp/main/resources/webapp/WEB-INF/keystore.jks

# We need a running webapp
source config/download_and_configure_tomcat.sh
sleep 10 # wait for tomcat to come on line
# Add necessary keys to the test properties.
echo 'i.am.a.dev = true'                        >> $TEST_PROPS # Show 500 error messages.
echo 'security.keystore.password = intermine'   >> $TEST_PROPS
echo 'security.privatekey.password = intermine' >> $TEST_PROPS
echo 'security.privatekey.alias = SELF'         >> $TEST_PROPS
echo 'jwt.verification.strategy = ANY'          >> $TEST_PROPS
echo 'jwt.publicidentity = ci'                  >> $TEST_PROPS
sh testmodel/setup.sh # requires PSQL_USER to be set correctly.
sleep 10 # wait for the webapp to come on line

