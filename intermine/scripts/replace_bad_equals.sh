#!/bin/bash

# This script searches recursively for java source files
# and replaces unsafe string equals tests of the type
# foo.equals("bar") with the null-pointer safe 
# "bar".equals(foo). Back-ups of the original files are made
# with the ".orig" suffix.

if [ $1 ]; then
    echo "This script searches recursively for .java files and replaces unsafe string equals patterns in those files, making back-ups with the suffix '.orig'"
    exit
fi
files=$(find . -type f -name '*.java' -exec grep -l '.equals("' {} +)
for f in $files; do
    perl -nlp -i.orig -e 's/((?:\w|\.|_|\w\(.*?\))+)\.equals\((".*?")/$2.equals($1/' $f
done
