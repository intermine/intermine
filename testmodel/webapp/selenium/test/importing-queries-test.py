# -*- coding: utf-8 -*-

import unittest
import os
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.alert import Alert
from selenium.webdriver.support.ui import Select, WebDriverWait
from test.testmodeltestcase import TestModelTestCase as Super

class QueryImportTestCase(Super):

    def setUp(self):
        Super.setUp(self)
        self.browser.get(self.base_url + '/importQueries.do')

    def import_query(self, xml):
        self.assertIn('Import Query', self.browser.title)
        self.elem('#xml').send_keys(xml)
        self.elem('#importQueriesForm input[type="submit"]').click()

    def assert_error_msg_contains(self, message):
        wait = WebDriverWait(self.browser, 10)
        error_notification = self.elem('#error_msg')
        wait.until(EC.visibility_of(error_notification))
        self.assertIn(message, error_notification.text)

    def test_bad_xml(self):
        bad_query = '<query model="testmodel" view="Employee.name Bank.name"/>'
        self.import_query(bad_query)
        self.assert_error_msg_contains('Multiple root classes')

    def test_empty_form(self):
        self.import_query('')
        self.assert_error_msg_contains('end of file')

    def test_from_file(self):
        file_input = self.elem('#file')
        file_input.send_keys(os.path.abspath('data/employee-query.xml'))

        self.elem('#importQueriesForm input[type="submit"]').click()
        self.assertEquals('Employee', self.elem('.typeSelected').text)
        self.assertEquals(3, len(self.browser.find_elements_by_class_name('viewpath')))

    def test_bad_file(self):
        file_input = self.elem('#file')
        file_input.send_keys(os.path.abspath('data/bad-query.xml'))
        self.elem('#importQueriesForm input[type="submit"]').click()

        self.assert_error_msg_contains('Multiple root classes')

