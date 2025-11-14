#!/bin/bash

# set up solr for testmine
# testmine's setup script populates these empty indexes

set -euo pipefail

if [ "$#" != "1" ]; then
   echo "Usage: $0 <workspace_dir>"
   exit 2
fi

WORKSPACE_DIR=$1

cd "$WORKSPACE_DIR"

SOLR_VERSION=8.6.2
SOLR_PACKAGE=solr-${SOLR_VERSION}.tgz
SOLR_DIR=solr-${SOLR_VERSION}
SOLR=${SOLR_DIR}/bin/solr

create_solr_core() {
    local core_name=$1

    local status

    status=$(curl -s "http://localhost:8983/solr/admin/cores?action=STATUS&core=${core_name}" | jq --arg core_name "${core_name}" '.status[$core_name]')

    if [ "$status" = "{}" ]; then
        ${SOLR} create -c "${core_name}"
    else
        echo "Solr core ${core_name} already exists"
    fi
}

if [ ! -d $SOLR_DIR ]; then
    if [ ! -f $SOLR_PACKAGE ]; then
        wget http://archive.apache.org/dist/lucene/solr/${SOLR_VERSION}/${SOLR_PACKAGE}
    fi

    tar xzf $SOLR_PACKAGE
fi

${SOLR} restart
create_solr_core intermine-search
create_solr_core intermine-autocomplete
