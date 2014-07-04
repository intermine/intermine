from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from imuser import IMUser
import config
import unittest, time, re

class AccountLogin(unittest.TestCase):
    def setUp(self):

        self.user = IMUser("zombie-testing-account-login@intermine.org");
        self.driver = webdriver.Firefox()
        self.driver.implicitly_wait(30)
        self.base_url = config.base_url
        self.verificationErrors = []
        self.accept_next_alert = True
    
    def test_account_login(self):

        driver = self.driver
        driver.get(self.base_url + "/begin.do")
        driver.find_element_by_link_text("Log in").click()
        driver.find_element_by_name("username").clear()
        driver.find_element_by_name("username").send_keys(self.user.name)
        driver.find_element_by_name("password").clear()
        driver.find_element_by_name("password").send_keys(self.user.password)
        driver.find_element_by_name("action").click()

        print "raw value:" + driver.find_element_by_css_selector("#loginbar li:nth-child(2)").text

        self.assertEqual(str(self.user.name), driver.find_element_by_css_selector("#loginbar li:nth-child(2)").text)
    
    def is_element_present(self, how, what):
        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
    
    def is_alert_present(self):
        try: self.driver.switch_to_alert()
        except NoAlertPresentException, e: return False
        return True
    
    def close_alert_and_get_its_text(self):
        try:
            alert = self.driver.switch_to_alert()
            alert_text = alert.text
            if self.accept_next_alert:
                alert.accept()
            else:
                alert.dismiss()
            return alert_text
        finally: self.accept_next_alert = True
    
    def tearDown(self):
        self.user.delete()
        self.driver.quit()
        self.assertEqual([], self.verificationErrors)
        

if __name__ == "__main__":
    unittest.main()
