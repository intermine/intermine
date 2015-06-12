# -*- coding: utf-8 -*-

from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC

from test.testmodeltestcase import TestModelTestCase as Super

def on_page(selector):
    return EC.presence_of_element_located((By.CSS_SELECTOR, selector))

class QueryBuilderTestCase(Super):

    def setUp(self):
        Super.setUp(self)
        self.browser.get(self.base_url + '/customQuery.do')

    def run_and_expect(self, n):
        self.find_and_click('#showResult')
        self.assertRowCountIs(n)

    def assertRowCountIs(self, n):
        selector = '.im-table-container tbody tr'
        self.wait().until(on_page(selector))
        def get_elem_count(): return len(self.elems(selector))
        self.wait().until(lambda d: n == get_elem_count())
        self.assertEquals(n, get_elem_count())

