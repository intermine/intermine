#!/bin/bash

set -euo pipefail

if [ "$#" != "4" ]; then
   echo "Usage: $0 <workspace_dir> <python executable> <client> <testmodel_url>"
   exit 1
fi

WORKSPACE_DIR=$1
PYTHON=$2
CLIENT=$3
export TESTMODEL_URL=$4

cd "${WORKSPACE_DIR}"/client-${CLIENT}

# client tests expect TESTMODEL_URL to be set up correctly.

if [ "$CLIENT" = "JS" ]; then
    if [ -z "$(which npm)" ]; then
        echo "Cannot run tests -- npm is not available"
        exit 1
    fi

    # Installing acorn before everything else prevents a bizarre error
    # where the installation fails saying it's missing acorn
    # The error only happens in a fresh environment without node_modules and
    # bower_components installed, so devs don't usually see the error,
    # but Travis always does.
    # try to no install acorn anymore
    # npm install acorn
    npm install # installs deps
    grunt test  # runs tests

elif [ "$CLIENT" = "PY" ]; then

    ${PYTHON} -m pip install -r requirements.txt
    ${PYTHON} setup.py test
    ${PYTHON} setup.py livetest

fi
