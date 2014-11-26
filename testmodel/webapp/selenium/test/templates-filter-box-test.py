from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re, os

class TemplatesFilterBox(Super):
    def setUp(self):
        Super.setUp(self)
    
    def test_templates_filter_box(self):

        browser = self.browser
        browser.get(self.base_url + "/templates.do")
        browser.find_element_by_id("filterText").clear()

        # Filter for CEO templates
        browser.find_element_by_id("filterText").send_keys("CEO")

        # Confirm that we still have our CEO template
        for i in range(60):
            try:
                if browser.find_element_by_id("all_templates_template_item_line_CEO_Rivals").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        # Confirm that a non-CEO template disappeared
        for i in range(60):
            try:
                if not browser.find_element_by_id("all_templates_template_item_line_ManagerLookup").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        # Filter by "Underwater"
        browser.find_element_by_id("filterText").clear()
        browser.find_element_by_id("filterText").send_keys("Underwater")

        # Confirm that we still have our Underwater template
        for i in range(60):
            try:
                if browser.find_element_by_id("all_templates_template_item_score_Underwater_People").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")


        # Confirm that a CEO went away
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