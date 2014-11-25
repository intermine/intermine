import time

from test.querybuildertestcase import QueryBuilderTestCase
import test.conditions as conditions
import test.actions as actions

from selenium.webdriver.common.alert import Alert

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
        self.assertEquals(2, len(self.elems('#modifyQueryForm tbody tr')))
        self.elem('#selected_history_1').click()
        self.elem('#delete_button').click()
        Alert(self.browser).accept()
        self.assertEquals(1, len(self.elems('#modifyQueryForm tbody tr')))

    def load_queries_into_history(self):
        query_1 = """<query model="testmodel" view="Bank.debtors.debt" sortOrder="Bank.debtors.debt asc"/>"""
        query_2 = """
            <query model="testmodel" view="Bank.name Bank.debtors.debt" sortOrder="Bank.debtors.debt asc">
                <constraint path="Bank.debtors.debt" op="&gt;" value="35,000,000"/>
            </query>
        """
        import_query = "Import query from XML"
        click = actions.click
        xml_text_field = conditions.find_by_id('xml')
        show_result_button = conditions.find_by_id('showResult')

        # Load queries into session history.
        for q in [query_1, query_2]:
            self.browser.get(self.base_url + '/customQuery.do')
            with self.wait_for(lambda d: d.find_element_by_link_text(import_query)) as link:
                link.click()
            with self.wait_for(xml_text_field) as field:
                field.send_keys(q)
                self.elem('#importQueriesForm input[type="submit"]').click()
            with self.wait_for(show_result_button) as button:
                button.click()
        self.browser.get(self.base_url + '/customQuery.do')

    def test_run_query_in_query_history(self):
        self.load_queries_into_history()

        self.elem('#modifyQueryForm tbody tr:nth-child(2) td:nth-child(7) span.fakelink:nth-child(1)').click()
        self.assertRowCountIs(16)

    def test_edit_query_in_query_history(self):
        self.load_queries_into_history()

        self.elem('#modifyQueryForm tbody tr:nth-child(2) td:nth-child(7) span.fakelink:nth-child(2)').click()

        self.wait().until(lambda d: 'query' in d.title.lower())

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
