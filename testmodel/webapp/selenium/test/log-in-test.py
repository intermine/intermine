import unittest
from selenium import webdriver
from selenium.webdriver.common.keys import Keys
from test.browsertestcase import BrowserTestCase

class LoginTestCase(BrowserTestCase):

    def setUp(self):
        BrowserTestCase.setUp(self)
        self.browser.get('http://localhost:8080/intermine-demo/begin.do')

    def testLogin(self):
        login_link = self.browser.find_element_by_link_text('Log in')
        self.assertIsNotNone(login_link)
        login_link.click()

        username = self.browser.find_element_by_name('username')
        self.assertIsNotNone(username)
        username.send_keys('intermine-test-user')
        password = self.browser.find_element_by_name('password')
        self.assertIsNotNone(password)
        password.send_keys('intermine-test-user-password')

        submit = self.browser.find_element_by_name('action')
        submit.click()

        logged_in_as = self.browser.find_element_by_css_selector('#loginbar li:nth-child(2)')
        self.assertEqual('intermine-test-user', logged_in_as.text)


