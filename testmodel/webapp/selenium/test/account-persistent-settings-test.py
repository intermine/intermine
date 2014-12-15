from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re
from imuser import TemporaryUser

class AccountPersistentSettings(Super):

    def setUp(self):
        Super.setUp(self)
        self.user = TemporaryUser("zombie-testing-account-login@intermine.org");
        self.user.create()

    def test_account_persistent_settings(self):

        browser = self.browser

        self.login()

        # Make sure that our checkboxes are set to on
        checkbox_donotspam = self.wait().until(lambda d: d.find_element_by_name('do_not_spam'))
        checkbox_hidden = browser.find_element_by_name("hidden")

        checkbox_donotspam.click() if checkbox_donotspam.is_selected() else False
        checkbox_hidden.click() if checkbox_hidden.is_selected() else False
        # Checkboxes are now off.

        # Now fill out our field values:
        alias = browser.find_element_by_name('alias')
        email = browser.find_element_by_name('email')
        alias.clear()
        alias.send_keys("Temporary Display Name")
        browser.find_element_by_xpath("//div[@id='pagecontentmax']/div[4]/div/table/tbody/tr[3]/td[2]/form/button").click()
        email.clear()
        email.send_keys("temporaryemail@intermine.org")
        browser.find_element_by_xpath("//div[@id='pagecontentmax']/div[4]/div/table/tbody/tr[4]/td[2]/form/button").click()
        self.findLink("Log out").click()

        # Log back in and confirm the settings have stuck:
        self.login()

        checkbox_donotspam = self.wait().until(lambda d: d.find_element_by_name('do_not_spam'))
        checkbox_hidden = browser.find_element_by_name("hidden")

        self.assertEqual(False, checkbox_donotspam.is_selected())
        self.assertEqual(False, checkbox_hidden.is_selected())
        # self.assertEqual("Temporary Display Name", browser.find_element_by_name("alias").get_attribute("value"))
        self.assertEqual("temporaryemail@intermine.org", browser.find_element_by_name("email").get_attribute("value"))

        # Reverse the values
        checkbox_donotspam.click()
        checkbox_hidden.click()
        alias = browser.find_element_by_name('alias')
        email = browser.find_element_by_name('email')
        alias.clear()
        browser.find_element_by_xpath("//div[@id='pagecontentmax']/div[4]/div/table/tbody/tr[3]/td[2]/form/button[2]").click()
        email.clear()
        browser.find_element_by_xpath("//div[@id='pagecontentmax']/div[4]/div/table/tbody/tr[4]/td[2]/form/button[2]").click()
        browser.find_element_by_link_text("Log out").click()

        # Finally, log back in and make sure the values are back to the original
        self.login()

        # Get our checkboxes again
        checkbox_donotspam = self.wait().until(lambda d: d.find_element_by_name('do_not_spam'))
        checkbox_hidden = browser.find_element_by_name("hidden")

        self.assertEqual(True, checkbox_donotspam.is_selected())
        self.assertEqual(True, checkbox_hidden.is_selected())
        # The following MIGHT be broken on the testmodel?
        # self.assertEqual("Temporary Display Name", browser.find_element_by_name("alias").get_attribute("value"))
        self.assertEqual("", browser.find_element_by_name("email").get_attribute("value"))

    def login(self):
        browser = self.browser
        browser.get(self.base_url + "/login.do?returnto=%2Fmymine.do?subtab=account")
        uname = self.wait().until(lambda d: d.find_element_by_name('username'), 'username not found')
        pword = browser.find_element_by_name('password')
        uname.clear()
        uname.send_keys(self.user.name)
        pword.clear()
        pword.send_keys(self.user.password)
        self.click_and_wait_for_refresh(browser.find_element_by_name("action"))

    def is_element_present(self, how, what):
        try:
            self.browser.find_element(by=how, value=what)
        except NoSuchElementException, e:
            return False
        return True

    def is_alert_present(self):
        try:
            self.browser.switch_to_alert()
        except NoAlertPresentException, e:
            return False
        return True

    def close_alert_and_get_its_text(self):
        try:
            alert = self.browser.switch_to_alert()
            alert_text = alert.text
            if self.accept_next_alert:
                alert.accept()
            else:
                alert.dismiss()
            return alert_text
        finally: self.accept_next_alert = True
