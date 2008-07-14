#!/bin/bash
mkdir -p build
javac -d ./build -classpath ../../lib/commons-codec-1.3.jar:../../lib/commons-httpclient-3.0.jar:../../lib/commons-logging-1.1.1.jar:../../lib/intermine-client.jar:../../lib/intermine-pathquery.jar:../../lib/log4j.jar ./src/samples/template/TemplateClient.java
java -classpath ./build:../../lib/commons-codec-1.3.jar:../../lib/commons-httpclient-3.0.jar:../../lib/commons-logging-1.1.1.jar:../../lib/intermine-client.jar:../../lib/intermine-pathquery.jar:../../lib/log4j.jar samples.template.TemplateClient
