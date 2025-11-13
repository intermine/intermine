#!/bin/bash

PROPDIR=$HOME/.intermine
TEST_PROPS=$PROPDIR/intermine-test.properties
TESTMODEL_PROPS=$PROPDIR/testmodel.properties
BIO_PROPS=$PROPDIR/intermine-bio-test.properties

copy_properties() {
    local source=$1
    local target=$2

    echo "#--- creating $target"
    cp "$source" "$target"
    sed -i -e "s/PSQL_HOST/${PSQL_HOST}/" "$target"
    sed -i -e "s/PSQL_USER/${PSQL_USER}/" "$target"
    sed -i -e "s/PSQL_PWD/${PSQL_PWD}/" "$target"

}

mkdir -p "$PROPDIR"
copy_properties "${WORKSPACE_DIR}"/config/ci.properties "$TEST_PROPS"
copy_properties "${WORKSPACE_DIR}"/config/testmodel.properties "$TESTMODEL_PROPS"
copy_properties "${WORKSPACE_DIR}"/config/ci-bio.properties "$BIO_PROPS"
