from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re
from imuser import TemporaryUser

class AccountLogin(Super):

    def setUp(self):
        Super.setUp(self)
        self.user = TemporaryUser("zombie-testing-account-login@intermine.org");
        self.user.create()

    def test_account_login(self):

        browser = self.browser
        browser.get(self.base_url + "/begin.do")
        self.click_and_wait_for_refresh(self.findLink("Log in"))
        browser.find_element_by_name("username").clear()
        browser.find_element_by_name("username").send_keys(self.user.name)
        browser.find_element_by_name("password").clear()
        browser.find_element_by_name("password").send_keys(self.user.password)
        self.click_and_wait_for_refresh(browser.find_element_by_name("action"))

        # Long emails are truncated and appended with an ellipsis,
        # so we can't assert by comparing user.name to what's on the DOM.
        # Look for Log out link instead?
        log_out = self.findLink("Log out")

        self.assertTrue(log_out.is_displayed())

    def tearDown(self):
        self.user.delete()
