import unittest
from test import WebserviceTest

from intermine.webservice import *
from intermine.lists.list import List

class TestLists(WebserviceTest): # pragma: no cover

    def setUp(self):
        self.service = Service(self.get_test_root())
         
    def testGetLists(self):
        """Should be able to get lists from a service"""
        self.assertEqual(self.service.get_list_count(), 3)

        list_a = self.service.get_list("test-list-1")
        self.assertTrue(list_a.description, "An example test list")
        self.assertEqual(list_a.size, 42)
        self.assertEqual(list_a.count, 42)
        self.assertEqual(len(list_a), 42)
        self.assertEqual(list_a.title, "test1")
        self.assertTrue(list_a.is_authorized)
        self.assertEqual(list_a.list_type, "Employee")
        self.assertEqual(list_a.tags, frozenset(["tag1", "tag2", "tag3"]))

        list_a = self.service.get_list("test-list-2")
        self.assertTrue(list_a.description, "Another example test list")
        self.assertEqual(list_a.size, 7)
        self.assertEqual(len(list_a), 7)
        self.assertEqual(list_a.count, 7)
        self.assertTrue(not list_a.is_authorized)
        self.assertEqual(list_a.tags, frozenset([]))

        list_c = self.service.get_list("test-list-3")
        self.assertTrue(list_c.description, "Yet Another example test list")
        self.assertEqual(list_c.size, 8)
        self.assertTrue(list_c.is_authorized)

        def alter_size():
            list_a.size = 10
        def alter_type():
            list_a.list_type = "foo"
        self.assertRaises(AttributeError, alter_size)
        self.assertRaises(AttributeError, alter_type)

    def testBadListConstruction(self):
        args = {}
        self.assertRaises(ValueError, lambda: List(**args))

    def tearDown(self):
        s = self.service
        s.__del__()
        
