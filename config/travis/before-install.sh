#!/bin/bash

set -e

if [ "$TEST_SUITE" = "selenium" ]; then
    # Start X server so we can run 
    sh -e /etc/init.d/xvfb start
    # Configure the screen size.
    /sbin/start-stop-daemon --start \
                            --quiet \
                            --pidfile /tmp/custom_xvfb_99.pid \
                            --make-pidfile \
                            --background \
                            --exec /usr/bin/Xvfb -- \
                            :99 -ac -screen 0 1280x1024x16
fi

