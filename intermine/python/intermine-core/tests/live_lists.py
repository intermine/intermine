import os
import sys
sys.path.insert(0, os.getcwd())

import unittest
from intermine.webservice import Service

def emp_rows_without_ids(bag):
    return [row[:3] + row[4:] for row in bag.to_query().rows()]


# This is coded all as one enormous test so that we can do
# a universal clean-up at the end.
class LiveListTest(unittest.TestCase):

    TEST_ROOT = "http://localhost/intermine-test/service"
    TEST_USER = "intermine-test-user"
    TEST_PASS = "intermine-test-user-password"

    # Expected rows
    KARIM = [37, '4', False, 'Karim']
    JENNIFER_SCHIRRMANN = [55, '9', False, 'Jennifer Schirrmann']
    JENNIFER = [45, '8', True, 'Jennifer']
    JEAN_MARC = [53, '0', True,  'Jean-Marc']
    VINCENT = [29, '3', True, 'Vincent']
    INA = [39, '8', True, 'Ina']
    ALEX = [43, '0', True, 'Alex']
    DELPHINE = [47, '9', False, 'Delphine']
    BRENDA = [54, '2', False, 'Brenda']
    KEITH = [56, None, False, 'Keith Bishop']
    CAROL = [62, '3', True, 'Carol']
    GARETH = [61, '8', True, 'Gareth Keenan']
    DAVID = [41, None, False, 'David Brent']
    FRANK = [44, None, False, u'Frank M\xf6llers']
    JULIETTE = [71, None, False, 'Juliette Lebrac']
    BWAH_HA = [74, None, False, "Bwa'h Ha Ha"]

    SERVICE = Service(TEST_ROOT, TEST_USER, TEST_PASS)

    LADIES_NAMES = ["Brenda", "Zop", "Carol", "Quux", "Jennifer", "Delphine", "Ina"]
    GUYS_NAMES = 'Alex Karim "Gareth Keenan" Foo Bar "Keith Bishop" Vincent Baz'
    EMPLOYEE_FILE = "tests/data/test-identifiers.list"
    TYPE = 'Employee'

    maxDiff = None

    def __init__(self, name):
        unittest.TestCase.__init__(self, name)
        self.initialListCount = self.SERVICE.get_list_count()

    def testListsFromFlyMine(self):
        s = Service("www.flymine.org/query")
        all_lists = s.get_all_lists()
        possible_statuses = set(["CURRENT", "TO_UPGRADE", "NOT_CURRENT"])
        got = set((l.status for l in all_lists))
        self.assertTrue(got <= possible_statuses)

    def testListTagAdding(self):
        s = self.SERVICE
        t = self.TYPE;
        l = s.create_list(self.GUYS_NAMES, t, description="Id string")
        self.assertEqual(set(), l.tags)
        l.add_tags("a-tag", "b-tag")
        self.assertEqual(set(["a-tag", "b-tag"]), l.tags)

    def testListTagRemoval(self):
        s = self.SERVICE
        t = self.TYPE;
        tags = ["a-tag", "b-tag", "c-tag"]
        l = s.create_list(self.GUYS_NAMES, t, description="Id string", tags = tags)
        self.assertEqual(set(tags), l.tags)
        l.remove_tags("a-tag", "c-tag")
        self.assertEqual(set(["b-tag"]), l.tags)
        l.remove_tags("b-tag", "d-tag")
        self.assertEqual(set(), l.tags)

    def testListTagUpdating(self):
        s = self.SERVICE
        t = self.TYPE;
        l = s.create_list(self.GUYS_NAMES, t, description="Id string")
        self.assertEqual(set(), l.tags)
        self.assertEqual(["a-tag", "b-tag"], s._list_manager.add_tags(l, ["a-tag", "b-tag"]))
        self.assertEqual(set(), l.tags)
        l.update_tags()
        self.assertEqual(set(["a-tag", "b-tag"]), l.tags)

    def testLists(self):
        t = self.TYPE;
        s = self.SERVICE
        self.assertTrue(s.get_list_count() > 0)

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
            LiveListTest.KARIM, LiveListTest.DAVID, LiveListTest.FRANK,
            LiveListTest.JEAN_MARC, LiveListTest.JENNIFER_SCHIRRMANN
        ]

        got = [row[:3] + row[4:] for row in l.to_query().rows()]
        self.assertEqual(got, expected)

        # Test iteration:
        got = set([x.age for x in l])
        expected_ages = set([37, 41, 44, 53, 55])
        self.assertEqual(expected_ages, got)

        self.assertTrue(l[0].age in expected_ages)
        self.assertTrue(l[-1].age in expected_ages)
        self.assertTrue(l[2].age in expected_ages)
        self.assertRaises(IndexError, lambda: l[5])
        self.assertRaises(IndexError, lambda: l[-6])
        self.assertRaises(IndexError, lambda: l["foo"])

        # Test intersections

        listA = s.create_list(self.GUYS_NAMES, t)
        listB = s.create_list(self.EMPLOYEE_FILE, t)

        intersection = listA & listB
        self.assertEqual(intersection.size, 1)
        expected = [LiveListTest.KARIM]
        self.assertEqual(emp_rows_without_ids(intersection), expected)

        q = s.new_query("Employee").where("age", ">", 50)
        intersection = listB & q
        self.assertEqual(intersection.size, 2)
        expected = [LiveListTest.JEAN_MARC, LiveListTest.JENNIFER_SCHIRRMANN]
        self.assertEqual(emp_rows_without_ids(intersection), expected)

        prev_name = listA.name
        prev_desc = listA.description
        listA &= listB
        self.assertEqual(listA.size, 1)
        got = emp_rows_without_ids(listA)
        expected = [LiveListTest.KARIM]
        self.assertEqual(got, expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

        # Test unions
        listA = s.create_list(self.GUYS_NAMES, t, tags=["tagA", "tagB"])
        listB = s.create_list(self.LADIES_NAMES, t)

        union = listA | listB
        self.assertEqual(union.size, 10)
        expected = [
            LiveListTest.VINCENT, LiveListTest.KARIM, LiveListTest.INA,
            LiveListTest.ALEX, LiveListTest.JENNIFER, LiveListTest.DELPHINE,
            LiveListTest.BRENDA, LiveListTest.KEITH, LiveListTest.GARETH,
            LiveListTest.CAROL
        ]
        got = [row[:3] + row[4:] for row in union.to_query().rows()]
        self.assertEqual(got, expected)

        union = listA + listB
        self.assertEqual(union.size, 10)
        self.assertEqual(emp_rows_without_ids(union), expected)

        # Test appending

        prev_name = listA.name
        prev_desc = listA.description
        listA += listB
        self.assertEqual(listA.size, 10)
        self.assertEqual(listA.tags, set(["tagA", "tagB"]))
        fromService = s.get_list(listA.name)
        self.assertEqual(listA.tags, fromService.tags)
        self.assertEqual(emp_rows_without_ids(listA), expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

        listA = s.create_list(self.GUYS_NAMES, t, description="testing appending")
        prev_name = listA.name
        prev_desc = listA.description
        listA += self.LADIES_NAMES
        self.assertEqual(listA.size, 10)
        self.assertEqual(emp_rows_without_ids(listA), expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)
        self.assertEqual(len(listA.unmatched_identifiers), 5)

        listA = s.create_list(self.GUYS_NAMES, t, description="testing appending")
        prev_name = listA.name
        prev_desc = listA.description
        listA += self.EMPLOYEE_FILE
        self.assertEqual(listA.size, 9)
        expected = [
            LiveListTest.VINCENT,
            LiveListTest.KARIM,
            LiveListTest.DAVID,
            LiveListTest.ALEX,
            LiveListTest.FRANK,
            LiveListTest.JEAN_MARC,
            LiveListTest.JENNIFER_SCHIRRMANN,
            LiveListTest.KEITH,
            LiveListTest.GARETH
        ]
        self.assertEqual(emp_rows_without_ids(listA), expected)
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
            LiveListTest.VINCENT, LiveListTest.KARIM, LiveListTest.INA,
            LiveListTest.DAVID, LiveListTest.ALEX,
            LiveListTest.FRANK, LiveListTest.JENNIFER, LiveListTest.DELPHINE,
            LiveListTest.JEAN_MARC, LiveListTest.BRENDA, LiveListTest.JENNIFER_SCHIRRMANN,
            LiveListTest.KEITH, LiveListTest.GARETH, LiveListTest.CAROL
        ]
        self.assertEqual(emp_rows_without_ids(listA), expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

        listA = s.create_list(self.GUYS_NAMES, t)
        q = s.new_query()
        q.add_view("Employee.id")
        q.add_constraint("Employee.age", '>', 65)

        prev_name = listA.name
        prev_desc = listA.description
        listA += [listA, listB, listC, q]
        self.assertEqual(listA.size, 16)
        expected = [
            LiveListTest.VINCENT, LiveListTest.KARIM, LiveListTest.INA,
            LiveListTest.DAVID, LiveListTest.ALEX,
            LiveListTest.FRANK, LiveListTest.JENNIFER, LiveListTest.DELPHINE,
            LiveListTest.JEAN_MARC, LiveListTest.BRENDA, LiveListTest.JENNIFER_SCHIRRMANN,
            LiveListTest.KEITH, LiveListTest.GARETH, LiveListTest.CAROL,
            LiveListTest.JULIETTE, LiveListTest.BWAH_HA
        ]
        self.assertEqual(emp_rows_without_ids(listA), expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

        # Test diffing
        listA = s.create_list(self.GUYS_NAMES, t)
        listB = s.create_list(self.EMPLOYEE_FILE, t)

        diff = listA ^ listB
        self.assertEqual(diff.size, 8)
        expected = [
            LiveListTest.VINCENT,
            LiveListTest.DAVID, LiveListTest.ALEX,
            LiveListTest.FRANK,
            LiveListTest.JEAN_MARC, LiveListTest.JENNIFER_SCHIRRMANN,
            LiveListTest.KEITH, LiveListTest.GARETH
        ]
        self.assertEqual(emp_rows_without_ids(diff), expected)

        prev_name = listA.name
        prev_desc = listA.description
        listA ^= listB
        self.assertEqual(listA.size, 8)
        self.assertEqual(emp_rows_without_ids(listA), expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

        # Test subtraction
        listA = s.create_list(self.GUYS_NAMES, t, tags=["subtr-a", "subtr-b"])
        listB = s.create_list(self.EMPLOYEE_FILE, t)

        subtr = listA - listB
        self.assertEqual(subtr.size, 4)
        expected = [
             LiveListTest.VINCENT, LiveListTest.ALEX, LiveListTest.KEITH,LiveListTest.GARETH
        ]
        got = [row[:3] + row[4:] for row in subtr.to_query().rows()]
        self.assertEqual(got, expected)

        prev_name = listA.name
        prev_desc = listA.description
        listA -= listB
        self.assertEqual(listA.size, 4)
        self.assertEqual(listA.tags, set(["subtr-a", "subtr-b"]))
        self.assertEqual(emp_rows_without_ids(listA), expected)
        self.assertEqual(prev_name, listA.name)
        self.assertEqual(prev_desc, listA.description)

        # Test subqueries
        with_cc_q = s.model.Bank.where("corporateCustomers.id", "IS NOT NULL")
        with_cc_l = s.create_list(with_cc_q)

        self.assertEqual(2, s.model.Bank.where(s.model.Bank ^ with_cc_q).count())
        self.assertEqual(2, s.model.Bank.where(s.model.Bank ^ with_cc_l).count())

        self.assertEqual(3, s.model.Bank.where(s.model.Bank < with_cc_q).count())
        self.assertEqual(3, s.model.Bank.where(s.model.Bank < with_cc_l).count())

        boring_q = s.new_query("Bank")
        boring_q.add_constraint("Bank", "NOT IN", with_cc_q)
        self.assertEqual(2, boring_q.count())

        boring_q = s.new_query("Bank")
        boring_q.add_constraint("Bank", "NOT IN", with_cc_l)
        self.assertEqual(2, boring_q.count())

        # Test query overloading

        no_comps = s.new_query("Bank") - with_cc_q
        self.assertEqual(2, no_comps.size)

        no_comps = s.new_query("Bank") - with_cc_l
        self.assertEqual(2, no_comps.size)

        all_b = s.new_query("Bank") | with_cc_q
        self.assertEqual(5, all_b.size)

        all_b = s.new_query("Bank") | with_cc_l
        self.assertEqual(5, all_b.size)

        # Test enrichment

        favs = s.l("My-Favourite-Employees")
        enriched_contractors = map(lambda x: x.identifier, favs.calculate_enrichment('contractor_enrichment', maxp = 1.0))
        self.assertEqual(enriched_contractors, ['Vikram'])

    def tearDown(self):
        s = self.SERVICE
        s.__del__()
        self.assertEqual(self.SERVICE.get_list_count(), self.initialListCount)

class LiveListTestWithTokens(LiveListTest):
    SERVICE = Service(LiveListTest.TEST_ROOT, token="test-user-token")

if __name__ == '__main__':
    unittest.main()

