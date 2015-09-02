#!/bin/bash
# Compiles sample source and runs sample.

if [ $# = 0 ]; then 
    echo "Usage: compile-run sample_name"
    exit;
fi;

FILE=$1/src/samples/$1.java
if !(test -e ${FILE}); then 
    echo "File ${FILE} doesn't exist.";
    echo "Usage: compile-run sample_name"
    exit; 
fi;

CLASSPATH=../../../../objectstore/main/dist/intermine-objectstore.jar:../../../../pathquery/main/dist/intermine-pathquery.jar:../../../../web/main/lib/commons-codec-1.9.jar:../../../../webservice/client/main/lib/commons-httpclient-3.0.jar:../../../../web/main/lib/commons-logging-1.1.1.jar:../../main/dist/intermine-webservice-client.jar:../main/lib/log4j.jar:../../../../integrate/main/lib/stax-api-1.0.jar:../../../../integrate/main/lib/stax-ri-1.0.jar:../../../../model/main/lib/antlr-2.7.6-caching.jar:../../../../model/main/lib/commons-lang-2.6.jar:../../../../model/main/lib/commons-io-1.2.jar:../../../../model/main/lib/json20110106.jar:../../../../model/main/dist/intermine-model.jar:../../../../model/main/lib/log4j.jar

cd $1;
mkdir -p build;
javac -d ./build -classpath ${CLASSPATH} ./src/samples/$1.java && \
java -classpath ./build:${CLASSPATH} samples.$@;
