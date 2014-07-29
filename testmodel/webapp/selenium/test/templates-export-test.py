from test.testmodeltestcase import TestModelTestCase as Super
import unittest
import hashlib

class TemplateExport(Super):

    def setUp(self):
        Super.setUp(self)

    def test_template_export(self):
        browser = self.browser
        browser.get(self.base_url + "/template.do?name=employeeByName&scope=all")

        # Export the template
        browser.find_element_by_link_text("export XML").click()

        # get an md5 of the export source
        val = hashlib.md5(browser.page_source)

        # Assert that the value of the template export is what we expect
        self.assertEqual('36c236f3615b14aa72252eaf2204a2f9', val.hexdigest())