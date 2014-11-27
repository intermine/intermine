from selenium.webdriver.support.ui import Select
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC

from test.querybuildertestcase import QueryBuilderTestCase as Super

def on_page(selector):
    return EC.presence_of_element_located((By.CSS_SELECTOR, selector))

class AddConstraintSetTest(Super):

    def test_add_constraint_set_and(self):
        query = """
        <query model="testmodel" view="Bank.name Bank.debtors.debt" sortOrder="Bank.debtors.debt ASC" >
        </query>
        """
        # Using join as the whitespace is significant here.
        expected_query = "\n".join([
            ' '.join([
                '<query',
                'name=""',
                'model="testmodel"',
                'view="Bank.name Bank.debtors.debt"',
                'longDescription=""',
                'sortOrder="Bank.debtors.debt asc"',
                'constraintLogic="A and B">'
            ]),
            '  <constraint path="Bank.debtors.debt" code="A" op="&gt;" value="35,000,000"/>',
            '  <constraint path="Bank.name" code="B" op="=" value="Gringotts"/>',
            '</query>'
        ])
        self.findLink("Import query from XML").click()
        xml = self.wait().until(on_page('#xml'))
        xml.send_keys(query)
        self.elem('#importQueriesForm input[type="submit"]').click()

        # Add constraint: Bank.debtors.debt > 35e6
        debtors = self.wait().until(EC.presence_of_element_located((By.ID, 'img_Bank.debtors')))
        debtors.click()
        add_constraint = self.wait().until(on_page('a[title="Add a constraint to debt"]'))
        add_constraint.click()
        attr_5 = self.wait().until(on_page('#attribute5'))
        Select(attr_5).select_by_visible_text(">")
        self.elem('#attribute8').send_keys('35,000,000')
        self.elem('#attributeSubmit').click()

        # Add constraint: Bank.name = Gringotts
        self.find_and_click('a[title="Add a constraint to name"]')
        attr_7 = self.wait().until(on_page('#attribute7'))
        Select(attr_7).select_by_visible_text("Gringotts")
        save_constraint = self.elem('#attributeSubmit')
        save_constraint.click()
        self.wait().until_not(lambda d: d.find_element_by_id('attributeSubmit'), "#attributeSubmit did not go away")

        # Check that the query is as expected.
        self.click_and_wait_for_refresh('a[title="Export this query as XML"]')
        self.assertEquals(expected_query.strip(), self.elem('body').text)
        self.browser.back()

        # Check that the results are as expected.
        self.wait().until(on_page('#showResult')).click()
        self.assertRowCountIs(2)

    def test_add_constraint_set_or(self):
        query = """
        <query model="testmodel" view="Bank.name Bank.debtors.debt" sortOrder="Bank.debtors.debt ASC" >
        </query>
        """
        expected_query = "\n".join([
            ' '.join([
                '<query',
                'name=""',
                'model="testmodel"',
                'view="Bank.name Bank.debtors.debt"',
                'longDescription=""',
                'sortOrder="Bank.debtors.debt asc"',
                'constraintLogic="A or B">'
            ]),
            '  <constraint path="Bank.debtors.debt" code="A" op="&gt;" value="35,000,000"/>',
            '  <constraint path="Bank.name" code="B" op="=" value="Gringotts"/>',
            '</query>'
        ])

        # Perform actions.
        self.findLink("Import query from XML").click()
        self.wait().until(on_page('#xml')).send_keys(query)
        self.elem('#importQueriesForm input[type="submit"]').click()
        # Add constraint: Bank.debtors.debt > 35e6
        debtors = self.wait().until(EC.presence_of_element_located((By.ID, 'img_Bank.debtors')))
        debtors.click()
        add_constraint = self.wait().until(on_page('a[title="Add a constraint to debt"]'))
        add_constraint.click()
        attr_5 = self.wait().until(on_page('#attribute5'))
        Select(attr_5).select_by_visible_text(">")
        self.elem('#attribute8').send_keys('35,000,000')
        self.elem('#attributeSubmit').click()

        # Add constraint: Bank.name = Gringotts
        self.find_and_click('a[title="Add a constraint to name"]')
        attr_7 = self.wait().until(on_page('#attribute7'))
        Select(attr_7).select_by_visible_text("Gringotts")
        self.elem('#attributeSubmit').click()
        # Switch the constraint logic to A or B
        self.elem('#constraintLogic').click()
        logic = self.elem('#expr')
        logic.clear()
        logic.send_keys('A or B')
        self.elem('#editconstraintlogic').click()

        # Check that the query is as expected.
        self.click_and_wait_for_refresh('a[title="Export this query as XML"]')
        self.assertEquals(expected_query.strip(), self.elem('body').text)
        self.browser.back()
        # Check that the results are as expected.
        self.wait().until(on_page('#showResult')).click()
        self.assertRowCountIs(24)

