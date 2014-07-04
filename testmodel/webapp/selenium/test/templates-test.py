import unittest
from test.testmodeltestcase import TestModelTestCase as Super

class TemplateTestCase(Super):

    def setUp(self):
        Super.setUp(self)
        self.browser.get(self.base_url + '/templates.do')
        self.template_name = "Search for Managers"

    def elem(self, selector):
        return self.browser.find_element_by_css_selector(selector)

    def testTemplatesPageTitle(self):
        self.assertIn('Template queries', self.browser.title)

    def findLink(self):
        return self.browser.find_element_by_link_text(self.template_name)

    def testFindTemplate(self):
        template_link = self.findLink()
        self.assertIsNotNone(template_link, "Expected to find link")
        self.assertTrue(
            template_link.is_displayed(),
            "Expected link to be visible to user"
        )

    def testRunTemplate(self):
        template_link = self.findLink()
        template_link.click()
        self.assertIn(self.template_name, self.browser.title)
        button = self.elem("#smallGreen.button input")
        self.assertIsNotNone(button, "Expected to find button to run template")
        button.click()
        summary = self.elem(".im-table-summary")
        self.assertIsNotNone(button, "Expected to find a summary of the template results")
        self.assertEqual("Showing 1 to 2 of 2 rows", summary.text)

