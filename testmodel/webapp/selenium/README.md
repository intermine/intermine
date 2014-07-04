Browser UI Tests using Selenium WebDriver
==================================================

This directory contains tests for running automated browser based user interface
testing using [Selenium](http://docs.seleniumhq.org/). In particular the tests
in this directory are meant to cover the main interface features of the generic
web-application.

These tests are written in Python using `unittest` as the main test framework,
`selenium` to interact with the Selenium webdriver and `nose` as a test runner.

Running the Tests
---------------------

The tests are normally run as part of the CI test suite. They can also be run
locally, and of course should be when new tests are added or changes are made to
existing tests.

.. code-block:: bash
    
    virtualenv venv
    source venv/bin/activate
    pip install -r requirements.txt
    nosetests

Required Environment Variables
---------------------

global use:
TESTMODEL_URL - The base URL of the web application. Ex: http://localhost:8080/intermine-demo

account-login-openid-test.py:
TESTMODEL_OPENID_NAME - The username of a Gmail account
TESTMODEL_OPENID_PASSWORD - The password of a Gmail account