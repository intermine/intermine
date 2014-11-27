# -*- coding: utf-8 -*-

from test.testmodeltestcase import TestModelTestCase as Super


class QueryBuilderTestCase(Super):

    def setUp(self):
        Super.setUp(self)
        self.browser.get(self.base_url + '/customQuery.do')

    def run_and_expect(self, n):
        self.elem('#showResult').click()
        summary = self.elem(".im-table-summary")
        self.assertRowCountIs(n)

    def assertRowCountIs(self, n):
        self.assertEquals(n, len(self.elems('.im-table-container tbody tr')))


