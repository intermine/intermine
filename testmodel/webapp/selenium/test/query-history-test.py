import time

from test.querybuildertestcase import QueryBuilderTestCase

from selenium.webdriver.common.alert import Alert
from selenium.webdriver.support.ui import WebDriverWait

class QueryHistoryTest(QueryBuilderTestCase):

    def test_query_history(self):
        self.load_queries_into_history()
        wait = WebDriverWait(self.browser, 15)
        wait.until(lambda d: 'query' in d.title.lower())

        self.assertIn('Custom query', self.browser.title)
        self.assertEquals(2, len(self.elems('#modifyQueryForm tbody tr')))
        self.assertEquals('query_2', self.elem('#modifyQueryForm tbody tr:nth-child(2) td:nth-child(2)').text)
        root = self.elem('#modifyQueryForm tbody tr:nth-child(2) .historySummaryRoot').text
        self.assertEquals('Bank', root)
        showing = self.elems('#modifyQueryForm tbody tr:nth-child(2) .historySummaryShowing')
        self.assertEquals(2, len(showing))
        self.assertEquals(['Name', 'Debt'], [s.text for s in showing])

    def test_delete_query_from_history(self):
        self.load_queries_into_history()
        self.assertEquals(2, len(self.elems('#modifyQueryForm tbody tr')))
        self.elem('#selected_history_1').click()
        self.elem('#delete_button').click()
        Alert(self.browser).accept()
        self.assertEquals(1, len(self.elems('#modifyQueryForm tbody tr')))

    def load_queries_into_history(self):
        wait = WebDriverWait(self.browser, 15)
        query_1 = ''.join([
            '<query model="testmodel" view="Bank.debtors.debt" sortOrder="Bank.debtors.debt asc">',
            '</query>'
            ])
        query_2 = ''.join([
            '<query model="testmodel" view="Bank.name Bank.debtors.debt" sortOrder="Bank.debtors.debt asc">',
            '<constraint path="Bank.debtors.debt" op="&gt;" value="35,000,000"/>',
            '</query>'
            ])
        import_query = "Import query from XML"
        # Load queries into session history.
        for q in [query_1, query_2]:
            self.browser.get(self.base_url + '/customQuery.do')
            wait.until(lambda driver: driver.find_element_by_link_text(import_query))
            self.findLink(import_query).click()
            wait.until(lambda driver: driver.find_element_by_id('xml'))
            self.elem('#xml').send_keys(q)
            wait.until(lambda driver: driver.find_element_by_id('showResult'))
            self.elem('#importQueriesForm input[type="submit"]').click()
            self.elem('#showResult').click()
        self.browser.get(self.base_url + '/customQuery.do')

    def test_run_query_in_query_history(self):
        self.load_queries_into_history()

        self.elem('#modifyQueryForm tbody tr:nth-child(2) td:nth-child(7) span.fakelink:nth-child(1)').click()
        self.assertRowCountIs(16)

    def test_edit_query_in_query_history(self):
        self.load_queries_into_history()

        self.elem('#modifyQueryForm tbody tr:nth-child(2) td:nth-child(7) span.fakelink:nth-child(2)').click()

        wait = WebDriverWait(self.browser, 15)
        wait.until(lambda d: 'query' in d.title.lower())

        self.assertIn('Query builder', self.browser.title)
        self.assertEquals('Bank', self.elem('.typeSelected').text)
        # Edit a constraint.
        self.elem('img[title="Edit this constraint"]').click()
        con_value = self.elem('#attribute8')
        con_value.clear()
        con_value.send_keys('40,000,000')
        self.elem('#attributeSubmit').click()
        # Check results.
        self.elem('#showResult').click()
        self.assertRowCountIs(15)

    def test_export_query_in_query_history(self):
        self.load_queries_into_history()
        expected_query = '\n'.join([
            ' '.join([
                '<query',
                'name="query_2"',
                'model="testmodel"',
                'view="Bank.name Bank.debtors.debt"',
                'longDescription=""',
                'sortOrder="Bank.debtors.debt asc">'
                ]),
            '  <constraint path="Bank.debtors.debt" op="&gt;" value="35,000,000"/>',
            '</query>'])

        self.elem('#modifyQueryForm tbody tr:nth-child(2) td:nth-child(7) span.fakelink:nth-child(3)').click()
        self.assertEquals(expected_query, self.elem('body').text)
