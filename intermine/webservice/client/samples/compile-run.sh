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

cd $1;
mkdir -p build;
javac -d ./build -classpath ../../lib/commons-codec-1.3.jar:../../lib/commons-httpclient-3.0.jar:../../lib/commons-logging-1.1.1.jar:../../lib/intermine-client.jar:../../lib/intermine-pathquery.jar:../../lib/log4j.jar ./src/samples/$1.java;
java -classpath ./build:../../lib/commons-codec-1.3.jar:../../lib/commons-httpclient-3.0.jar:../../lib/commons-logging-1.1.1.jar:../../lib/intermine-client.jar:../../lib/intermine-pathquery.jar:../../lib/log4j.jar samples.$1;
