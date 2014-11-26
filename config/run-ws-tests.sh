#!/bin/bash

set -e

if [ -z $(which npm) ]; then
    echo "Cannot run tests -- npm is not available"
    exit 1
fi

cd imjs
npm install
npm test

