import sys
import os
sys.path.insert(0, os.getcwd())

import unittest

from intermine.webservice import Registry

class LiveRegistryTest(unittest.TestCase):

    def testAccessRegistry(self):
        r = Registry()
        self.assertTrue("flymine" in r)
        self.assertTrue(r["flymine"].version > 5)

if __name__ == '__main__':
    unittest.main()

