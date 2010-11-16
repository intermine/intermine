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

CLASSPATH=../../lib/intermine-objectstore.jar:../../lib/intermine-pathquery.jar:../../lib/commons-codec-1.3.jar:../../lib/commons-httpclient-3.0.jar:../../lib/commons-logging-1.1.1.jar:../../lib/intermine-client.jar:../../lib/log4j.jar:../../lib/stax-api-1.0.jar:../../lib/stax-ri-1.0.jar:../../lib/antlr-2.7.6-caching.jar:../../lib/commons-lang-2.3.jar

cd $1;
mkdir -p build;
javac -d ./build -classpath ${CLASSPATH} ./src/samples/$1.java && \
java -classpath ./build:${CLASSPATH} samples.$@;
