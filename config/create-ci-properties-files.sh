#!/bin/bash

PROPDIR=$HOME/.intermine
TEST_PROPS=$PROPDIR/intermine-test.properties
TESTMODEL_PROPS=$PROPDIR/testmodel.properties
BIO_PROPS=$PROPDIR/intermine-bio-test.properties
SED_SCRIPT='s/PG_USER/postgres/'

mkdir -p $PROPDIR
echo "#--- creating $TEST_PROPS"
cp config/ci.properties   $TEST_PROPS
sed -i.bak -e $SED_SCRIPT $TEST_PROPS

echo "#--- creating $TESTMODEL_PROPS"
cp $TEST_PROPS $TESTMODEL_PROPS

echo "#--- creating $BIO_PROPS"
cp config/ci-bio.properties $BIO_PROPS
sed -i.bak -e "$SED_SCRIPT" $BIO_PROPS
