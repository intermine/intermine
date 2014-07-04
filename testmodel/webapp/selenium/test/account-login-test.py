from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re
from imuser import IMUser


class AccountLogin(Super):

    def setUp(self):
        Super.setUp(self)
        self.user = IMUser("zombie-testing-account-login@intermine.org");

    def test_account_login(self):

        browser = self.browser
        browser.get(self.base_url + "/begin.do")
        browser.find_element_by_link_text("Log in").click()
        browser.find_element_by_name("username").clear()
        browser.find_element_by_name("username").send_keys(self.user.name)
        browser.find_element_by_name("password").clear()
        browser.find_element_by_name("password").send_keys(self.user.password)
        browser.find_element_by_name("action").click()

        # Long emails are truncated and appended with an ellipsis,
        # so we can't assert by comparing user.name to what's on the DOM.
        # Look for Log out link instead?
        for i in range(60):
            try:
                if self.is_element_present(By.LINK_TEXT, "Log out"): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        self.assertTrue(self.is_element_present(By.LINK_TEXT, "Log out"))

    def is_element_present(self, how, what):
        try: self.browser.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
    
   
    def tearDown(self):
        self.user.delete()

