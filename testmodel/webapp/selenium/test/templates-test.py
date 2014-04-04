import unittest
from selenium import webdriver
from selenium.webdriver.common.keys import Keys

class TemplateTestCase(unittest.TestCase):

    def setUp(self):
        self.browser = webdriver.Firefox()
        self.addCleanup(self.browser.quit)
        self.browser.get('http://localhost:8080/intermine-demo/templates.do')

    def elem(self, selector):
        return self.browser.find_element_by_css_selector(selector)

    def testTemplatesPageTitle(self):
        self.assertIn('Template queries', self.browser.title)

    def testFindTemplate(self):
        template_link = self.browser.find_element_by_link_text("Search for Managers")
        self.assertIsNotNone(template_link, "Expected to find link")
        self.assertTrue(template_link.is_displayed(), "Expected link to be visible to user")

    def testRunTemplate(self):
        template_link = self.browser.find_element_by_link_text("Search for Managers")
        template_link.click()
        self.assertIn('Search for Managers', self.browser.title)
        button = self.elem("#smallGreen.button input")
        self.assertIsNotNone(button, "Expected to find button to run template")
        button.click()
        summary = self.elem(".im-table-summary")
        self.assertIsNotNone(button, "Expected to find a summary of the template results")
        self.assertEqual("Showing 1 to 2 of 2 rows", summary.text)

