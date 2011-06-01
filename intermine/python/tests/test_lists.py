import unittest
from test import WebserviceTest

from intermine.webservice import *

class TestLists(WebserviceTest):

    def setUp(self):
        self.service = Service(self.get_test_root())
         
    def testGetLists(self):
        """Should be able to get lists from a service"""
        self.assertEqual(self.service.get_list_count(), 3)

        list_a = self.service.get_list("test-list-1")
        self.assertTrue(list_a.description, "An example test list")
        self.assertEqual(list_a.size, 42)
        self.assertTrue(list_a.is_authorized)

        list_a = self.service.get_list("test-list-2")
        self.assertTrue(list_a.description, "Another example test list")
        self.assertEqual(list_a.size, 7)
        self.assertTrue(not list_a.is_authorized)

        list_c = self.service.get_list("test-list-3")
        self.assertTrue(list_c.description, "Yet Another example test list")
        self.assertEqual(list_c.size, 8)
        self.assertTrue(list_c.is_authorized)

    def tearDown(self):
        s = self.service
        s.__del__()
        
