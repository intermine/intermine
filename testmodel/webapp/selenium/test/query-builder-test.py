import unittest
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import Select, WebDriverWait
from test.testmodeltestcase import TestModelTestCase as Super

EXPECTED_TYPES = ['Bank', 'Broke', 'Employment Period', 'Has Address',
    'Has Secretarys', 'Important Person', 'Random Interface', 'Range', 'Secretary', 'Thing', 'Types']

class QueryBuilderTestCase(Super):

    def setUp(self):
        Super.setUp(self)
        self.browser.get(self.base_url + '/begin.do')
        self.elem("#query > a").click()

    def test_on_right_page(self):
        self.assertIn('Custom query', self.browser.title)

    def test_browse_data_model(self):
        link = self.findLink("Browse data model")
        self.assertIsNotNone(link)
        link.click()
        help_text = self.elem('.body > p').text
        self.assertIn("browse the tree", help_text)
        for type_name in EXPECTED_TYPES:
            self.assertIsNotNone(self.findLink(type_name))

        self.findLink('Bank').click()
        self.assertIn('Query builder', self.browser.title)
        self.assertEquals('Bank', self.elem('.typeSelected').text)

    def start_query_from_select(self):
        cls = 'Employee'
        Select(self.elem("#queryClassSelector")).select_by_visible_text(cls)
        self.elem("#submitClassSelect").click()
        self.assertIn('Query builder', self.browser.title)
        self.assertEquals(cls, self.elem('.typeSelected').text)
        self.elem('a[title="Show Employee in results"] > img.arrow').click()

    def test_query_tree(self):
        Select(self.elem("#queryClassSelector")).select_by_visible_text("Bank")
        self.elem("#submitClassSelect").click()
        self.assertIn('Query builder', self.browser.title)
        self.assertEquals('Bank', self.elem('.typeSelected').text)
        self.assertEquals('Name', self.elem('.attributeField').text)
        cc = self.browser.find_element_by_id('drag_Bank.corporateCustomers')
        self.assertEquals('Corporate Customers', cc.text)
        ds = self.browser.find_element_by_id('drag_Bank.debtors')
        self.assertEquals('Debtors', ds.text)
        # Add a view element.
        self.browser.find_element_by_id('img_Bank.debtors').click()
        self.elem('a[title="Show debt in results"] > img.arrow').click()
        self.assertEquals(1, len(self.browser.find_elements_by_class_name('viewpath')))
        self.elem('a[title="Show name in results"] > img.arrow').click()
        self.assertEquals(2, len(self.browser.find_elements_by_class_name('viewpath')))
        # Add a constraint
        self.elem('a[title="Add a constraint to debt"]').click()
        Select(self.elem("#attribute5")).select_by_visible_text(">")
        self.elem("#attribute8").clear()
        self.elem("#attribute8").send_keys("1000")
        self.elem("#attributeSubmit").click()
        constraints = self.elems('span.constraint')
        self.assertEquals(1, len(constraints))
        self.assertEquals('> 1000', constraints[0].text)
        self.elem('a[title="Export this query as XML"]').click()
        expected_query = """
<query name="" model="testmodel" view="Bank.debtors.debt Bank.name" longDescription="" sortOrder="Bank.debtors.debt asc">
  <constraint path="Bank.debtors.debt" op="&gt;" value="1000"/>
</query>
        """
        self.assertEquals(expected_query.strip(), self.elem('body').text)

    def test_import_query(self):
        link = self.findLink("Import query from XML")
        self.assertIsNotNone(link)
        link.click()
        self.assertIn('Import Query', self.browser.title)
        input_box = self.elem('#xml')
        self.assertIsNotNone(input_box)
        query = """
<query name="" model="testmodel" view="Bank.debtors.debt" longDescription="" sortOrder="Bank.debtors.debt asc">
  <constraint path="Bank.debtors.debt" op="&gt;" value="1000"/>
</query>
        """

        input_box.send_keys(query)
        self.assertEquals('true', self.elem('#file').get_attribute('disabled'))
        self.elem('#importQueriesForm input[type="submit"]').click()
        wait = WebDriverWait(self.browser, 10)
        wait.until(EC.title_contains('Query builder'))

        self.assertEquals('Bank', self.elem('.typeSelected').text)
        constraints = self.elems('span.constraint')
        self.assertEquals(1, len(constraints))
        self.assertEquals('> 1000', constraints[0].text)
        self.assertEquals(1, len(self.browser.find_elements_by_class_name('viewpath')))

    def test_login_to_view_saved(self):
        link = self.findLink("Login to view saved queries")
        self.assertIsNotNone(link)
