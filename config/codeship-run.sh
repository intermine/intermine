#!/bin/bash

touch failures.list

ant -f intermine/all/build.xml \
    clean fulltest checkstyle \ | tee >(grep FAILED >> failures.list)

cat failures.list

PSQL_USER=$PG_USER PSQL_PWD=$PG_PASSWORD sh testmode/setup.sh
sleep 10
(cd testmodel/webapp/selenium; nosetests)

if [ -z $S3_LOCATION ]; then
    echo no s3 location provided.
else
    echo uploading results to s3
    # Requires AWS config. See codeship docs.
    pip install awscli
    NOW=$(date --iso-8601=seconds | sed 's/:/-/g')
    ARCHIVE="test-results-${NOW}.tar.gz"
    tar -acf "$ARCHIVE" intermine/all/build/
    aws s3 cp "$ARCHIVE" s3://${S3_LOCATION}/"$ARCHIVE"
fi

test ! -s failures.list
