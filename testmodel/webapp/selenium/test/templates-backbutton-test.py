from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re, os

class TemplatesBackButton(Super):
    def setUp(self):
        Super.setUp(self)
    
    def test_templates_backbutton(self):

        browser = self.browser
        browser.get(self.base_url + "/templates.do")
        browser.find_element_by_id("filterText").clear()
        browser.find_element_by_id("filterText").send_keys("underwater")

        # Confirm that we have results
        for i in range(60):
            try:
                if browser.find_element_by_id("all_templates_template_item_line_Underwater_People").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        # Confirm that we do not have CEO
        for i in range(60):
            try:
                if not browser.find_element_by_id("all_templates_template_item_line_CEO_Rivals").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        # Click the link to underwater people
        browser.find_element_by_link_text("Underwater People").click()

        # Maybe it's a good idea to sleep here?
        time.sleep(1)

        # Go back one in the history
        browser.back()

        # Assert that our filter still has the value of "underwater"
        self.assertEqual("underwater", browser.find_element_by_id("filterText").get_attribute("value"))

        # Confirm that we have results
        for i in range(60):
            try:
                if browser.find_element_by_id("all_templates_template_item_line_Underwater_People").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        # Confirm that we do not have CEO
        for i in range(60):
            try:
                if not browser.find_element_by_id("all_templates_template_item_line_CEO_Rivals").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

    def is_element_present(self, how, what):
        try: self.browser.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True