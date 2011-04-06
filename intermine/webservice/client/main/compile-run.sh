#!/bin/bash
#
# Compiles the sample source and runs sample.
#
# ARGS: the path to the sample, as a class or file name
#
# EG:
#
#  ./compile-run flymine.Query
#
# OR:
#
#  ./compile-run flymine/Query.java
#
# LICENSE: This code is distributed under the LGPL


if [ $# = 0 ]; then
    echo "Usage: compile-run sample_name"
    exit;
fi;

case $1 in
    *.java) FILE=$1 && CLASS=$(echo $1 | sed 's/\.java$//' | sed 's/\//./g');;
    *) CLASS=$1 && FILE=$(echo $1 | sed 's/\./\//g').java;;
esac

if !(test -e ${FILE}); then
    echo "File ${FILE} doesn't exist.";
    echo "Usage: compile-run sample_name"
    exit;
fi;

# Puts all jars from ./lib dir on the classpath
for i in ./lib/*jar
    do
    CLASSPATH=$CLASSPATH:$i
done

mkdir -p build;
javac -d ./build -classpath ${CLASSPATH} ./$FILE && \
java -classpath ./build:${CLASSPATH} $CLASS;
