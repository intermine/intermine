import time

from selenium.webdriver.support.ui import Select, WebDriverWait

from test.querybuildertestcase import QueryBuilderTestCase as Super

class QueryTreeTest(Super):

    def test_query_tree(self):
        Select(self.elem("#queryClassSelector")).select_by_visible_text("Bank")
        self.elem("#submitClassSelect").click()
        time.sleep(3)
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
