#!/bin/bash

PROPDIR=$HOME/.intermine
TEST_PROPS=$PROPDIR/intermine-test.properties
TESTMODEL_PROPS=$PROPDIR/testmodel.properties
BIO_PROPS=$PROPDIR/intermine-bio-test.properties
SED_SCRIPT='s/PSQL_USER/test/'

mkdir -p $PROPDIR
echo "#--- creating $TEST_PROPS"
cp config/ci.properties $TEST_PROPS
sed -i -e $SED_SCRIPT $TEST_PROPS

echo "#--- creating $TESTMODEL_PROPS"
cp config/testmodel.properties $TESTMODEL_PROPS
sed -i -e $SED_SCRIPT $TESTMODEL_PROPS

echo "#--- creating $BIO_PROPS"
cp config/ci-bio.properties $BIO_PROPS
sed -i -e "$SED_SCRIPT" $BIO_PROPS
