#!/bin/bash
mkdir -p build
javac -d ./build -classpath ../../lib/stax-api-1.0.jar:../../lib/stax-ri-1.0.jar:../../lib/commons-codec-1.3.jar:../../lib/intermine-objectstore.jar:../../lib/commons-httpclient-3.0.jar:../../lib/commons-logging-1.1.1.jar:../../lib/intermine-client.jar:../../lib/intermine-pathquery.jar:../../lib/log4j.jar ./src/samples/query/QueryAPI.java
java -classpath ./build:../../lib/stax-api-1.0.jar:../../lib/stax-ri-1.0.jar:../../lib/commons-codec-1.3.jar:../../lib/intermine-objectstore.jar:../../lib/commons-httpclient-3.0.jar:../../lib/commons-logging-1.1.1.jar:../../lib/intermine-client.jar:../../lib/intermine-pathquery.jar:../../lib/log4j.jar samples.query.QueryAPI
