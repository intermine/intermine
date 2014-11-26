from selenium.webdriver.support.ui import Select

from test.querybuildertestcase import QueryBuilderTestCase as Super

class AddConstraintSetTest(Super):

    def test_add_constraint_set_and(self):
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
                'constraintLogic="A and B">'
            ]),
            '  <constraint path="Bank.debtors.debt" code="A" op="&gt;" value="35,000,000"/>',
            '  <constraint path="Bank.name" code="B" op="=" value="Gringotts"/>',
            '</query>'
        ])
        self.findLink("Import query from XML").click()
        self.elem('#xml').send_keys(query)
        self.elem('#importQueriesForm input[type="submit"]').click()
        # Add constraint: Bank.debtors.debt > 35e6
        self.browser.find_element_by_id('img_Bank.debtors').click()
        self.elem('a[title="Add a constraint to debt"]').click()
        Select(self.elem("#attribute5")).select_by_visible_text(">")
        self.elem('#attribute8').send_keys('35,000,000')
        self.elem('#attributeSubmit').click()
        # Add constraint: Bank.name = Gringotts
        self.elem('a[title="Add a constraint to name"]').click()
        Select(self.elem("#attribute7")).select_by_visible_text("Gringotts")
        self.elem('#attributeSubmit').click()
        # Check that the query is as expected.
        current_title = self.browser.title
        self.elem('a[title="Export this query as XML"]').click()
        self.wait().until(lambda d: d.title != current_title)
        self.assertEquals(expected_query.strip(), self.elem('body').text)
        self.browser.back()
        # Check that the results are as expected.
        self.elem('#showResult').click()
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
        self.elem('#xml').send_keys(query)
        self.elem('#importQueriesForm input[type="submit"]').click()
        # Add constraint: Bank.debtors.debt > 35e6
        self.browser.find_element_by_id('img_Bank.debtors').click()
        self.elem('a[title="Add a constraint to debt"]').click()
        Select(self.elem("#attribute5")).select_by_visible_text(">")
        self.elem('#attribute8').send_keys('35,000,000')
        self.elem('#attributeSubmit').click()
        # Add constraint: Bank.name = Gringotts
        self.elem('a[title="Add a constraint to name"]').click()
        Select(self.elem("#attribute7")).select_by_visible_text("Gringotts")
        self.elem('#attributeSubmit').click()
        # Switch the constraint logic to A or B
        self.elem('#constraintLogic').click()
        logic = self.elem('#expr')
        logic.clear()
        logic.send_keys('A or B')
        self.browser.find_element_by_id('editconstraintlogic').click()

        # Check that the query is as expected.
        self.elem('a[title="Export this query as XML"]').click()
        self.assertEquals(expected_query.strip(), self.elem('body').text)
        self.browser.back()
        # Check that the results are as expected.
        self.elem('#showResult').click()
        self.assertRowCountIs(24)

