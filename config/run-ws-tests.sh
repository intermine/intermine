#!/bin/bash

set -e

cd client

# client tests expect TESTMODEL_URL to be set up correctly.

if [ "$CLIENT" = "JS" ]; then
    if [ -z $(which npm) ]; then
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

    pip install -r requirements.txt
    python setup.py test
    python setup.py livetest

fi
