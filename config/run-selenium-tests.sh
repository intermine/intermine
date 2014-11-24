#!/bin/bash

set -e

cd testmodel/webapp/selenium

for t in test/*-test.py; do
    echo running $t
    nosetests "$t"
done

