import logging
import os
import unittest

from contextlib import contextmanager

home_dir = os.getenv('HOME')
logging.basicConfig(level = logging.DEBUG)

from selenium import webdriver
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities

TIMEOUT = 60

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
            capabilities["build"] = os.environ["TRAVIS_BUILD_NUMBER"]
            capabilities["tags"] = [os.environ["TRAVIS_PYTHON_VERSION"], "CI", "TRAVIS"]
            hub_url = "%s:%s@localhost:4445" % (sauce_user, sauce_key)
            driver = webdriver.Remote(desired_capabilities=capabilities, command_executor="http://%s/wd/hub" % hub_url)
        cls.browser = driver
        cls.browser.implicitly_wait(TIMEOUT)

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
