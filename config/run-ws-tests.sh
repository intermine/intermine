#!/bin/bash

set -e

cd client

# client tests expect TESTMODEL_URL to be set up correctly.

if [ "$CLIENT" = "JS" ]; then
    if [ -z $(which npm) ]; then
        echo "Cannot run tests -- npm is not available"
        exit 1
    fi

    npm install # installs deps and runs tests.

elif [ "$CLIENT" = "PY" ]; then

    # No dependencies.
    python setup.py test
    python setup.py livetest

fi
