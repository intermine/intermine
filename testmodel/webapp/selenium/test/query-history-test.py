import time

from test.querybuildertestcase import QueryBuilderTestCase
import test.conditions as conditions
import test.actions as actions

from selenium.webdriver.common.alert import Alert
from selenium.webdriver.support import expected_conditions as EC

class QueryHistoryTest(QueryBuilderTestCase):

    def test_query_history(self):
        self.load_queries_into_history()
        self.wait().until(conditions.in_title('query'))

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
        row_selector = '#modifyQueryForm tbody tr'

        def get_row_count():
            return len(self.elems(row_selector))

        there_are_two_rows = lambda d: get_row_count() == 2
        there_is_one_row = lambda d: get_row_count() == 1

        self.wait().until(there_are_two_rows, "Row count wrong")

        self.assertEquals(2, get_row_count())
        self.find_and_click('#selected_history_1')
        self.find_and_click('#delete_button')
        Alert(self.browser).accept()

        self.wait().until(there_is_one_row, "Row count unchanged")

    def load_queries_into_history(self):
        query_1 = """<query model="testmodel" view="Bank.debtors.debt" sortOrder="Bank.debtors.debt asc"/>"""
        query_2 = """
            <query model="testmodel" view="Bank.name Bank.debtors.debt" sortOrder="Bank.debtors.debt asc">
                <constraint path="Bank.debtors.debt" op="&gt;" value="35,000,000"/>
            </query>
        """
        import_query = "Import query from XML"
        xml_text_field = conditions.find_by_id('xml')

        # Load queries into session history.
        for q in [query_1, query_2]:
            self.browser.get(self.base_url + '/customQuery.do')
            link = self.wait().until(lambda d: d.find_element_by_link_text(import_query))
            link.click()
            self.wait().until(xml_text_field).send_keys(q)
            self.find_and_click('#importQueriesForm input[type="submit"]')
            self.find_and_click('#showResult')
        self.browser.get(self.base_url + '/customQuery.do')

    def test_run_query_in_query_history(self):
        self.load_queries_into_history()

        self.find_and_click('#modifyQueryForm tbody tr:nth-child(2) td:nth-child(7) span.fakelink:nth-child(1)')
        self.assertRowCountIs(16)

    def test_edit_query_in_query_history(self):
        self.load_queries_into_history()

        self.find_and_click('#modifyQueryForm tbody tr:nth-child(2) td:nth-child(7) span.fakelink:nth-child(2)')

        self.wait().until(lambda d: 'query' in d.title.lower())

        self.assertIn('Query builder', self.browser.title)
        self.assertEquals('Bank', self.elem('.typeSelected').text)
        # Edit a constraint.
        self.find_and_click('img[title="Edit this constraint"]')
        con_value = self.wait().until(conditions.find_by_id('attribute8'))
        self.wait().until(lambda d: con_value.is_displayed())
        con_value.clear()
        con_value.send_keys('40,000,000')
        self.elem('#attributeSubmit').click()
        # Check results.
        self.find_and_click('#showResult')
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

        selector = '#modifyQueryForm tbody tr:nth-child(2) td:nth-child(7) span.fakelink:nth-child(3)'
        self.click_and_wait_for_refresh(selector)
        self.assertEquals(expected_query, self.elem('body').text)
