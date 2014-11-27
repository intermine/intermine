import os
from test.browsertestcase import BrowserTestCase

DEFAULT_BASE = 'http://localhost:8080/intermine-demo'

base_url = os.getenv('TESTMODEL_BASE', DEFAULT_BASE)

class TestModelTestCase(BrowserTestCase):

    def setUp(self):
        BrowserTestCase.setUp(self)
        self.base_url = base_url
