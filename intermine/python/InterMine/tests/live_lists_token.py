import sys
import os
sys.path.insert(0, os.getcwd())

import unittest
from intermine.webservice import Service

class LiveListTest(unittest.TestCase):

    TEST_ROOT = "http://localhost/intermine-test/service"
    TEST_TOKEN = "a1v3V1X0f3hdmaybq0l6b7Z4eVG"

    SERVICE = Service(TEST_ROOT, token=TEST_TOKEN)

    LADIES_NAMES = ["Brenda", "Zop", "Carol", "Quux", "Jennifer", "Delphine", "Ina"]
    GUYS_NAMES = 'Alex Karim "Gareth Keenan" Foo Bar "Keith Bishop" Vincent Baz'
    EMPLOYEE_FILE = "tests/data/test-identifiers.list"
    TYPE = 'Employee'

    maxDiff = None


    def __init__(self, name):
        unittest.TestCase.__init__(self, name)
        self.initialListCount = None
    
    def testLists(self):
        t = self.TYPE;
        s = self.SERVICE
        self.assertTrue(s.get_list_count() > 0)
        self.initialListCount = s.get_list_count()

        l = s.create_list(self.LADIES_NAMES, t, description="Id list")
        self.assertEqual(l.unmatched_identifiers, set(["Zop", "Quux"]))
        self.assertEqual(l.size, 5)
        self.assertEqual(l.list_type, t)

        l = s.get_list(l.name)
        self.assertEqual(l.size, 5)
        self.assertEqual(l.list_type, t)

        l = s.create_list(self.GUYS_NAMES, t, description="Id string")
        self.assertEqual(l.unmatched_identifiers, set(["Foo", "Bar", "Baz"]))
        self.assertEqual(l.size, 5)
        self.assertEqual(l.list_type, "Employee")

        l = s.create_list(self.EMPLOYEE_FILE, t, description="Id file", tags=["Foo", "Bar"])
        self.assertEqual(l.unmatched_identifiers, set(["Not a good id"]))
        self.assertEqual(l.size, 5)
        self.assertEqual(l.list_type, "Employee")
        self.assertEqual(l.tags, set(["Foo", "Bar"]))

        q = s.new_query()
        q.add_view("Employee.id")
        q.add_constraint("Employee.department.name", '=', "Sales")
        l = s.create_list(q, description="Id query")
        self.assertEqual(l.unmatched_identifiers, set())
        self.assertEqual(l.size, 18)
        self.assertEqual(l.list_type, t)

        l.name = "renamed query"

        l2 = s.get_list("renamed query")
        self.assertEqual(str(l), str(l2))

        l3 = s.create_list(l)
        self.assertEqual(l3.size, l2.size)

        l.delete()
        self.assertTrue(s.get_list("renamed query") is None)

        l = s.create_list(self.EMPLOYEE_FILE, t)
        expected = [
            [30, u'6', False, u'Karim'],
            [33, u'1', False, u'Jennifer Schirrmann'],
            [58, u'6', True,  u'Jean-Marc'],
            [62, None, False, u'David Brent'],
            [68, None, False, u'Frank M\xf6llers'],
        ]

        got = [row[:3] + row[4:] for row in l.to_attribute_query().rows()]
        self.assertEqual(got, expected)
        

        # Test iteration:
        got = [x.age for x in l]
        self.assertEqual([30, 33, 58, 62, 68], got)

        self.assertEqual(30, l[0].age)
        self.assertEqual(68, l[-1].age)
        self.assertEqual(58, l[2].age)
        self.assertRaises(IndexError, lambda: l[5])
        self.assertRaises(IndexError, lambda: l[-6])
        self.assertRaises(IndexError, lambda: l["foo"])

        # Test intersections

        listA = s.create_list(self.GUYS_NAMES, t)
        listB = s.create_list(self.EMPLOYEE_FILE, t)

        intersection = listA & listB
        self.assertEqual(intersection.size, 1)
        expected = [[30, u'6', False, u'Karim']]
        got = [row[:3] + row[4:] for row in intersection.to_attribute_query().rows()]
        self.assertEqual(got, expected)

        q = s.new_query()
        q.add_view("Employee.id")
        q.add_constraint("Employee.age", '>', 60)
        intersection = listB & q
        self.assertEqual(intersection.size, 2)
        expected = [
            [62, None, False,u'David Brent'],
            [68, None, False,u'Frank M\xf6llers'],
        ]
        got = [row[:3] + row[4:] for row in intersection.to_attribute_query().rows()]
        self.assertEqual(got, expected)

        prev_name = listA.name
        prev_desc = listA.description
        listA &= listB
        self.assertEqual(listA.size, 1)
        got = [row[:3] + row[4:] for row in listA.to_attribute_query().rows()]
        expected = [[30, u'6', False, u'Karim']]
        self.assertEqual(got, expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

        # Test unions
        listA = s.create_list(self.GUYS_NAMES, t, tags=["tagA", "tagB"])
        listB = s.create_list(self.LADIES_NAMES, t)

        union = listA | listB
        self.assertEqual(union.size, 10)
        expected = [
            [30, u'6', False, u'Karim'],
            [31, u'4', True, u'Jennifer'],
            [39, None, False, u'Keith Bishop'],
            [49, u'6', True, u'Ina'],
            [51, u'4', True, u'Delphine'],
            [54, u'0', False, u'Vincent'],
            [57, u'5', False, u'Carol'],
            [63, u'9', False, u'Alex'],
            [64, u'1', True, u'Brenda'],
            [64, u'7', True, u'Gareth Keenan'],
        ]
        got = [row[:3] + row[4:] for row in union.to_attribute_query().rows()]
        self.assertEqual(got, expected)

        union = listA + listB
        self.assertEqual(union.size, 10)
        got = [row[:3] + row[4:] for row in union.to_attribute_query().rows()]
        self.assertEqual(got, expected)

        # Test appending

        prev_name = listA.name
        prev_desc = listA.description
        listA += listB
        self.assertEqual(listA.size, 10)
        self.assertEqual(listA.tags, set(["tagA", "tagB"]))
        fromService = s.get_list(listA.name)
        self.assertEqual(listA.tags, fromService.tags)
        got = [row[:3] + row[4:] for row in listA.to_attribute_query().rows()]
        self.assertEqual(got, expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

        listA = s.create_list(self.GUYS_NAMES, t, description="testing appending")
        prev_name = listA.name
        prev_desc = listA.description
        listA += self.LADIES_NAMES
        self.assertEqual(listA.size, 10)
        got = [row[:3] + row[4:] for row in listA.to_attribute_query().rows()]
        self.assertEqual(got, expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)
        self.assertEqual(len(listA.unmatched_identifiers), 5)

        listA = s.create_list(self.GUYS_NAMES, t, description="testing appending")
        prev_name = listA.name
        prev_desc = listA.description
        listA += self.EMPLOYEE_FILE
        self.assertEqual(listA.size, 9)
        expected = [
            [30, u'6', False, u'Karim'],
            [33, u'1', False, u'Jennifer Schirrmann'],
            [39, None, False, u'Keith Bishop'],
            [54, u'0', False, u'Vincent'],
            [58, u'6', True,  u'Jean-Marc'],
            [62, None, False, u'David Brent'],
            [63, u'9', False, u'Alex'],
            [64, u'7', True, u'Gareth Keenan'],
            [68, None, False, u'Frank M\xf6llers'],
        ]
        got = [row[:3] + row[4:] for row in listA.to_attribute_query().rows()]
        self.assertEqual(got, expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

        listA = s.create_list(self.GUYS_NAMES, t)
        listB = s.create_list(self.EMPLOYEE_FILE, t)
        listC = s.create_list(self.LADIES_NAMES, t)

        prev_name = listA.name
        prev_desc = listA.description
        listA += [listA, listB, listC]
        self.assertEqual(listA.size, 14)
        expected = [
            [30, u'6', False, u'Karim'],
            [31, u'4', True, u'Jennifer'],
            [33, u'1', False, u'Jennifer Schirrmann'],
            [39, None, False, u'Keith Bishop'],
            [49, u'6', True, u'Ina'],
            [51, u'4', True, u'Delphine'],
            [54, u'0', False, u'Vincent'],
            [57, u'5', False, u'Carol'],
            [58, u'6', True,  u'Jean-Marc'],
            [62, None, False, u'David Brent'],
            [63, u'9', False, u'Alex'],
            [64, u'1', True, u'Brenda'],
            [64, u'7', True, u'Gareth Keenan'],
            [68, None, False, u'Frank M\xf6llers'],
        ]
        got = [row[:3] + row[4:] for row in listA.to_attribute_query().rows()]
        self.assertEqual(got, expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

        listA = s.create_list(self.GUYS_NAMES, t)
        q = s.new_query()
        q.add_view("Employee.id")
        q.add_constraint("Employee.age", '>', 65)

        prev_name = listA.name
        prev_desc = listA.description
        listA += [listA, listB, listC, q]
        self.assertEqual(listA.size, 17)
        expected = [
            [30, u'6', False, u'Karim'],
            [31, u'4', True, u'Jennifer'],
            [33, u'1', False, u'Jennifer Schirrmann'],
            [39, None, False, u'Keith Bishop'],
            [49, u'6', True, u'Ina'],
            [51, u'4', True, u'Delphine'],
            [54, u'0', False, u'Vincent'],
            [57, u'5', False, u'Carol'],
            [58, u'6', True,  u'Jean-Marc'],
            [62, None, False, u'David Brent'],
            [63, u'9', False, u'Alex'],
            [64, u'1', True, u'Brenda'],
            [64, u'7', True, u'Gareth Keenan'],
            [66, None, False, u'Joel Liotard'],
            [68, None, False, u'Frank M\xf6llers'],
            [71, None, False, u'Jennifer Taylor-Clarke'],
            [72, None, False, u'Charles Miner']
        ]
        got = [row[:3] + row[4:] for row in listA.to_attribute_query().rows()]
        self.assertEqual(got, expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

        # Test diffing
        listA = s.create_list(self.GUYS_NAMES, t)
        listB = s.create_list(self.EMPLOYEE_FILE, t)

        diff = listA ^ listB
        self.assertEqual(diff.size, 8)
        expected = [
            [33, u'1', False, u'Jennifer Schirrmann'],
            [39, None, False, u'Keith Bishop'],
            [54, u'0', False, u'Vincent'],
            [58, u'6', True,  u'Jean-Marc'],
            [62, None, False, u'David Brent'],
            [63, u'9', False, u'Alex'],
            [64, u'7', True, u'Gareth Keenan'],
            [68, None, False, u'Frank M\xf6llers'],
        ]
        got = [row[:3] + row[4:] for row in diff.to_attribute_query().rows()]
        self.assertEqual(got, expected)

        prev_name = listA.name
        prev_desc = listA.description
        listA ^= listB
        self.assertEqual(listA.size, 8)
        got = [row[:3] + row[4:] for row in listA.to_attribute_query().rows()]
        self.assertEqual(got, expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

        # Test subtraction
        listA = s.create_list(self.GUYS_NAMES, t, tags=["subtr-a", "subtr-b"])
        listB = s.create_list(self.EMPLOYEE_FILE, t)

        subtr = listA - listB
        self.assertEqual(subtr.size, 4)
        expected = [
            [39, None, False, u'Keith Bishop'],
            [54, u'0', False, u'Vincent'],
            [63, u'9', False, u'Alex'],
            [64, u'7', True, u'Gareth Keenan'],
        ]
        got = [row[:3] + row[4:] for row in subtr.to_attribute_query().rows()]
        self.assertEqual(got, expected)

        prev_name = listA.name
        prev_desc = listA.description
        listA -= listB
        self.assertEqual(listA.size, 4)
        self.assertEqual(listA.tags, set(["subtr-a", "subtr-b"]))
        got = [row[:3] + row[4:] for row in listA.to_attribute_query().rows()]
        self.assertEqual(got, expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

    def tearDown(self):
        s = self.SERVICE
        s.__del__()
        self.assertEqual(self.SERVICE.get_list_count(), self.initialListCount)

if __name__ == '__main__':
    unittest.main()

