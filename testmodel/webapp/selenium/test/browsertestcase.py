import unittest
from selenium import webdriver

class BrowserTestCase(unittest.TestCase):

    def setUp(self):
        """Start a new browser session, and schedule the browser to be shutdown"""
        self.browser = webdriver.Firefox()
        self.browser.implicitly_wait(10)
        self.addCleanup(self.browser.quit)

    def elem(self, selector):
        """Alias for self.browser.find_element_by_css_selector"""
        return self.browser.find_element_by_css_selector(selector)

    def elems(self, selector):
        """Alias for self.browser.find_elements_by_css_selector"""
        return self.browser.find_elements_by_css_selector(selector)

    def findLink(self, name):
        """Alias for self.browser.find_element_by_link_text"""
        return self.browser.find_element_by_link_text(name)
