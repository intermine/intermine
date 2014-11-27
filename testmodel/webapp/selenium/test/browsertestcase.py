import logging
import os
import unittest

from contextlib import contextmanager

home_dir = os.getenv('HOME')
logging.basicConfig(level = logging.DEBUG)

from selenium import webdriver
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities

TIMEOUT = 30

sauce_user = os.getenv("SAUCE_USERNAME")
sauce_key = os.getenv("SAUCE_ACCESS_KEY")
travis_job = os.getenv("TRAVIS_JOB_NUMBER")

class BrowserTestCase(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        if sauce_user is None:
            p = webdriver.FirefoxProfile()
            p.set_preference('webdriver.log.file', home_dir + '/firefox_console')
            driver = webdriver.Firefox(p)
        else:
            capabilities = DesiredCapabilities.FIREFOX.copy()
            capabilities = {"tunnel-identifier": travis_job}
            capabilities["build"] = os.getenv("TRAVIS_BUILD_NUMBER")
            capabilities["tags"] = ["CI", "TRAVIS"]
            hub_url = "%s:%s@localhost:4445" % (sauce_user, sauce_key)
            driver = webdriver.Remote(desired_capabilities=capabilities, command_executor="http://%s/wd/hub" % hub_url)
        cls.browser = driver

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

    def click_and_wait_for_refresh(self, elem):
        """Given an element, it clicks it and waits for the page to reload"""
        prev_url = self.browser.current_url
        if hasattr(elem, 'click'):
            elem.click()
        else:
            self.elem(elem).click()
        self.wait().until(lambda d: d.current_url != prev_url, "Browser stayed at " + prev_url)

    @contextmanager
    def wait_for(self, find_element):
        """Wait for an element, then act on it"""
        self.wait().until(find_element)
        yield find_element(self.browser)

    def find_and(self, selector, action):
        action(self.wait().until(lambda d: d.find_element_by_css_selector(selector), selector + ' not found'))

    def find_and_click(self, selector):
        self.wait().until(lambda d: d.find_element_by_css_selector(selector), selector + ' not found').click()

    def wait_for_elem(self, selector):
        return self.wait().until(lambda d: d.find_element_by_css_selector(selector), selector + ' not found')
