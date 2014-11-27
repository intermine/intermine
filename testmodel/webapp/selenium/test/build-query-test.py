# -*- coding: utf-8 -*-

from selenium.webdriver.support.ui import Select

from test.querybuildertestcase import QueryBuilderTestCase as Super

class BuildQueryTest(Super):

    def test_build_query(self):
        Select(self.elem("#queryClassSelector")).select_by_visible_text("Employee")
        self.find_and_click("#submitClassSelect")
        self.find_and_click('a[title="Show name in results"]')
        self.find_and_click('a[title="Add a constraint to name"]')
        self.find_and("#attribute8", lambda e: e.clear())
        self.find_and("#attribute8", lambda e: e.send_keys(u"*ö*"))
        self.find_and_click("#attributeSubmit")
        self.run_and_expect(4)

