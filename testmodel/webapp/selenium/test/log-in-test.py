import unittest
from selenium import webdriver
from selenium.webdriver.common.keys import Keys
from test.testmodeltestcase import TestModelTestCase as Super

class LoginTestCase(Super):

    def setUp(self):
        Super.setUp(self)
        self.browser.get(self.base_url + '/begin.do')

    def testLogin(self):
        login_link = self.browser.find_element_by_link_text('Log in')
        self.assertIsNotNone(login_link)
        login_link.click()

        username = self.wait().until(lambda d: d.find_element_by_name('username'))
        username.send_keys('intermine-test-user')
        password = self.browser.find_element_by_name('password')
        password.send_keys('intermine-test-user-password')

        submit = self.browser.find_element_by_name('action')
        submit.click()

        self.assertLoggedInAs('intermine-test-user')

    def assertLoggedInAs(self, username):
        sel = '#loginbar li:nth-child(2)'
        logged_in_as = self.wait().until(lambda d: d.find_element_by_css_selector(sel))
        self.assertEqual('intermine-test-user', logged_in_as.text)

