import sys
import os
sys.path.insert(0, os.getcwd())

import unittest
from intermine.webservice import Service

class LiveListTest(unittest.TestCase):

    TEST_ROOT = "http://localhost/intermine-test/service"

    SERVICE = Service(TEST_ROOT)

    def testLazyReferenceFetching(self):
        results = self.SERVICE.select("Department.*").results()
        managers = map(lambda x: x.manager.name, results)
        expected = [
            'EmployeeA1', 'EmployeeB1', 'EmployeeB3', 
            'David Brent', 'Keith Bishop', 'Glynn Williams', 'Neil Godwin', 
            u'Sinan Tur\xe7ulu', 'Bernd Stromberg', 'Timo Becker', 'Dr. Stefan Heinemann', 'Burkhardt Wutke', u'Frank M\xf6llers', 
            'Michael Scott', 'Angela', 'Lonnis Collins', 'Meredith Palmer', 
            'Gilles Triquet', 'Jacques Plagnol Jacques', u'Didier Legu\xe9lec', 'Joel Liotard', 
            'Quote Leader', 'Separator Leader', 'Slash Leader', 'XML Leader']

        self.assertEqual(expected, managers)

    def testLazyCollectionFetching(self):
        results = self.SERVICE.select("Department.*").results()
        age_sum = reduce(lambda x, y: x + reduce(lambda a, b: a + b.age, y.employees, 0), results, 0)
        self.assertEqual(5798, age_sum)

if __name__ == '__main__':
    unittest.main()

