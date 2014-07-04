import unittest
from selenium import webdriver

class BrowserTestCase(unittest.TestCase):

    def setUp(self):
        self.browser = webdriver.Firefox()
        self.browser.implicitly_wait(10)
        self.addCleanup(self.browser.quit)