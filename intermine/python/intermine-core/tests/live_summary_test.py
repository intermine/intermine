import sys
import os
sys.path.insert(0, os.getcwd())

import unittest
from intermine.webservice import Service

class LiveSummaryTest(unittest.TestCase):

    TEST_ROOT = "localhost/intermine-test"
    SERVICE = Service(TEST_ROOT)

    QUERY = SERVICE.select("Employee.*", "department.name")

    def testNumericSummary(self):
        summary = self.QUERY.summarise("age")
        self.assertEqual(10, summary["min"])
        self.assertEqual(74, summary["max"])
        self.assertEqual(44.878787878787875, summary["average"])
        self.assertEqual(12.075481627447155, summary["stdev"])

    def testNonNumericSummary(self):
        summary = self.QUERY.summarise("fullTime")
        self.assertEqual(56, summary[True])
        self.assertEqual(76, summary[False])

        summary = self.QUERY.summarise("department.name")
        self.assertEqual(18, summary["Sales"])

    def testSummaryAsIterator(self):
        path = "department.name"
        q = self.QUERY
        results = q.results(summary_path = path)
        top = results.next()
        self.assertEqual("Accounting", top["item"])
        self.assertEqual(18, top["count"])

        self.assertEqual(top, q.first(summary_path = path))

    def testAliasing(self):
        q = self.QUERY
        self.assertEqual(q.summarise("age"), q.summarize("age"))

if __name__ == '__main__':
    unittest.main()

