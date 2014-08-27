#!/bin/bash

set -e

BUILD_REPORT_FILE="./build.tar.gz"

if [ -z $BUILD_REPORT_SERVICE ]; then
    echo "No service specified."
else
    echo Uploading build.
    rm -f $BUILD_REPORT_FILE
    tar -acf $BUILD_REPORT_FILE \
        --exclude '*.xml' \
        intermine/all/build/
    curl --basic \
        --user TRAVIS:$BUILD_STORAGE_TOKEN \
        -F report=@${BUILD_REPORT_FILE} $BUILD_REPORT_SERVICE
    echo upload complete
fi

