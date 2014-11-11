# -*- coding: utf-8 -*-

from selenium.webdriver.support.ui import Select

from test.querybuildertestcase import QueryBuilderTestCase as Super

class BuildQueryTest(Super):

    def test_build_query(self):
        Select(self.elem("#queryClassSelector")).select_by_visible_text("Employee")
        self.elem("#submitClassSelect").click()
        self.elem('a[title="Show name in results"]').click()
        self.elem('a[title="Add a constraint to name"]').click()
        self.elem("#attribute8").clear()
        self.elem("#attribute8").send_keys(u"*ö*")
        self.elem("#attributeSubmit").click()
        self.run_and_expect(4)

