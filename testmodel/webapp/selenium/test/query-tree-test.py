from selenium.webdriver.support.ui import Select

from test.querybuildertestcase import QueryBuilderTestCase as Super

class QueryTreeTest(Super):

    def test_add_views(self):
        self.start_query('Bank')
        # Add a view element or two
        self.add_view('Bank.debtors', 'debt', 'name')
        self.assertEquals(2, self.get_view_length())

    def get_view_length(self):
        return len(self.elems('.viewpath'))

    def add_view(self, base, *names):
        self.browser.find_element_by_id('img_' + base).click()
        view_len = self.get_view_length()

        for name in names:
            selector = 'a[title="Show {} in results"] > img.arrow'.format(name)
            self.find_and_click(selector)
            view_len += 1
            self.wait().until(lambda d: view_len == self.get_view_length())

    def start_query(self, root_name):
        Select(self.elem("#queryClassSelector")).select_by_visible_text(root_name)
        self.find_and_click("#submitClassSelect")
        self.wait().until(lambda d: 'builder' in d.title)

    def test_tree_root(self):
        self.start_query('Bank')
        self.assertIn('Query builder', self.browser.title)
        self.assertEquals('Bank', self.elem('.typeSelected').text)
        self.assertEquals('Name', self.elem('.attributeField').text)
        cc = self.browser.find_element_by_id('drag_Bank.corporateCustomers')
        self.assertEquals('Corporate Customers', cc.text)
        ds = self.browser.find_element_by_id('drag_Bank.debtors')
        self.assertEquals('Debtors', ds.text)

    def add_constaint(self, field, op, value):
        selector = 'a[title="Add a constraint to {}"]'.format(field)
        add_constraint = self.wait().until(lambda d: d.find_element_by_css_selector(selector))
        add_constraint.click()
        ops = self.wait().until(lambda d: d.find_element_by_css_selector('#attribute5'))
        Select(ops).select_by_visible_text(op)
        value_field = self.elem("#attribute8")
        value_field.clear()
        value_field.send_keys(value)
        self.find_and_click("#attributeSubmit")

    def test_add_constraints(self):
        self.start_query('Bank')

        # Add a constraint
        self.browser.find_element_by_id('img_Bank.debtors').click()
        self.add_constaint('debt', '>', '1000')

        self.wait().until(lambda d: len(self.elems('span.constraint')))
        constraints = self.elems('span.constraint')
        self.assertEquals(1, len(constraints))
        self.assertEquals('> 1000', constraints[0].text)

    def test_all_together(self):
        self.start_query('Bank')
        self.add_view('Bank.debtors', 'debt', 'name')
        self.add_constaint('debt', '>', '1000')
        self.click_and_wait_for_refresh('a[title="Export this query as XML"]')
        expected_query = """
<query name="" model="testmodel" view="Bank.debtors.debt Bank.name" longDescription="" sortOrder="Bank.debtors.debt asc">
  <constraint path="Bank.debtors.debt" op="&gt;" value="1000"/>
</query>
        """
        self.assertEquals(expected_query.strip(), self.elem('body').text)
