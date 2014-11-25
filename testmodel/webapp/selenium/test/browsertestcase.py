import logging
import os
import unittest

from contextlib import contextmanager

home_dir = os.getenv('HOME')
logging.basicConfig(level = logging.DEBUG)

from selenium import webdriver
from selenium.webdriver.support.ui import WebDriverWait

TIMEOUT = 60

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

    def wait(self):
        """Convenience for creating waits"""
        return WebDriverWait(self.browser, TIMEOUT)

    @contextmanager
    def wait_for(self, find_element):
        """Wait for an element, then act on it"""
        self.wait().until(find_element)
        yield find_element(self.browser)
