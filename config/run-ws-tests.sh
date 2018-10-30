#!/bin/bash

set -e

cd client

# client tests expect TESTMODEL_URL to be set up correctly.

if [ "$CLIENT" = "JS" ]; then
    if [ -z $(which npm) ]; then
        echo "Cannot run tests -- npm is not available"
        exit 1
    fi

    npm install acorn
    npm install

elif [ "$CLIENT" = "PY" ]; then

    pip install -r requirements.txt
    python setup.py test
    python setup.py livetest

fi
