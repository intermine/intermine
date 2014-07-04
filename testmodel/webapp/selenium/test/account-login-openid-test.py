from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re, os
from imuser import IMUser

class AccountLoginOpenID(Super):

    def setUp(self):
        Super.setUp(self)

    def test_account_login_openid(self):
        print "SKIPPED"
        return

        name = os.getenv('TESTMODEL_OPENID_NAME')
        password = os.getenv('TESTMODEL_OPENID_PASSWORD')

        # Runs test assuming that google is our openid provider
        browser = self.browser
        browser.get("https://accounts.google.com/Logout")
        browser.get(self.base_url + "/begin.do")
        browser.find_element_by_link_text("Log in").click()
        browser.find_element_by_css_selector("a.google").click()
        browser.find_element_by_id("Email").clear()
        browser.find_element_by_id("Email").send_keys(name)
        browser.find_element_by_id("Passwd").clear()
        browser.find_element_by_id("Passwd").send_keys(password)
        browser.find_element_by_id("signIn").click()
        
        # TBD until we can deploy testmodel on a server with access to openid
        return
        self.assertEqual("Log out", browser.find_element_by_link_text("Log out").text)
        self.assetLoggedIn()
        self.upload_list_and_save_it()

    def assetLoggedIn(self):
        self.assertEqual("Log out", browser.find_element_by_link_text("Log out").text)

    def upload_list_and_save_it(self):
        browser.get(self.base_url + "//bag.do?subtab=upload")
        browser.find_element_by_link_text("Lists").click()
        browser.find_element_by_link_text("Upload").click()
        Select(browser.find_element_by_id("typeSelector")).select_by_visible_text("Company")
        browser.find_element_by_id("pasteInput").click()
        browser.find_element_by_id("pasteInput").clear()
        browser.find_element_by_id("pasteInput").send_keys("CompanyA,CompanyB,Dunder-Mifflin")
        browser.find_element_by_id("submitBag").click()
        browser.find_element_by_id("newBagName").clear()
        browser.find_element_by_id("newBagName").send_keys("Company List 1")
        for i in range(60):
            try:
                if self.is_element_present(By.XPATH, "//*[@id=\"target\"]/div[1]/header/a"): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")
        browser.find_element_by_xpath("//*[@id=\"target\"]/div[1]/header/a").click()
        browser.get(self.base_url + "/bag.do?subtab=view.do")


        # Create a list and save it
