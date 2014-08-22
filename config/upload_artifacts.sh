#!/bin/bash

if [ -z ${ARTIFACTS_AWS_ACCESS_KEY_ID+x} ]; then
    echo No access key provided.
else
    echo Uploading build.
    tar -cf build.tar.gz intermine/all/build/
    travis-artifacts \
        upload \
        --path build.tar.gz
fi

