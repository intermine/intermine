#!/bin/bash

set -e

cd client

# client tests expect TESTMODEL_URL to be set up correctly.

if [ "$CLIENT" = "JS" ]; then
    if [ -z $(which npm) ]; then
        echo "Cannot run tests -- npm is not available"
        exit 1
    fi

    #Bower needs to be installed before all other modules
    npm install bower
    bower install
    # The next line used to be all we need, but something fails on bower
    # For inexplicable reasons. Installing bower on its own seems to fix this.
    npm install

elif [ "$CLIENT" = "PY" ]; then

    pip install -r requirements.txt
    python setup.py test
    python setup.py livetest

fi
