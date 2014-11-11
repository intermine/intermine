from test.querybuildertestcase import QueryBuilderTestCase as Super

class RunQueryTest(Super):

    def test_run_query(self):
        query = """
        <query model="testmodel"
               view="Bank.name Bank.debtors.debt" sortOrder="Bank.debtors.debt ASC" >
          <constraint path="Bank.debtors.debt" op="&gt;" value="25000000" />
        </query>
        """
        self.findLink("Import query from XML").click()
        self.elem('#xml').send_keys(query)
        self.elem('#importQueriesForm input[type="submit"]').click()
        self.run_and_expect(22)
