#!/bin/bash
# Compiles sample source and runs sample.

if [ $# = 0 ]; then 
    echo "Usage: compile-run sample_name"
    exit;
fi;

FILE=$1.java
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

cd $1;
mkdir -p build;
javac -d ./build -classpath ${CLASSPATH} ./$1.java && \
java -classpath ./build:${CLASSPATH} $@;
