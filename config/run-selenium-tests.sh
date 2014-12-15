#!/bin/bash

set -e

cd testmodel/webapp/selenium

nosetests --verbose --with-flaky

