import logging
import os
import unittest

home_dir = os.getenv('HOME')
logging.basicConfig(level = logging.DEBUG)

from selenium import webdriver

class BrowserTestCase(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        p = webdriver.FirefoxProfile()
        p.set_preference('webdriver.log.file', home_dir + '/firefox_console')
        cls.browser = webdriver.Firefox(p)
        cls.browser.implicitly_wait(30)

    @classmethod
    def tearDownClass(cls):
        cls.browser.quit()

    def setUp(self):
        """Start a new browser session, and schedule the browser to be shutdown"""
        self.browser = self.__class__.browser
        self.browser.delete_all_cookies()

    def elem(self, selector):
        """Alias for self.browser.find_element_by_css_selector"""
        return self.browser.find_element_by_css_selector(selector)

    def elems(self, selector):
        """Alias for self.browser.find_elements_by_css_selector"""
        return self.browser.find_elements_by_css_selector(selector)

    def findLink(self, name):
        """Alias for self.browser.find_element_by_link_text"""
        return self.browser.find_element_by_link_text(name)
