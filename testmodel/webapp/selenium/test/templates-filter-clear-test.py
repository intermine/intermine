from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re, os

class TemplatesFilterClear(Super):
    def setUp(self):
        Super.setUp(self)
    
    def test_templates_filter_clear(self):

        browser = self.browser
        browser.get(self.base_url + "/templates.do")
        browser.find_element_by_id("filterText").clear()

        # Confirm that we have results
        for i in range(60):
            try:
                if browser.find_element_by_id("all_templates_template_item_line_CEO_Rivals").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        # Make sure that all elements go away
        browser.find_element_by_id("filterText").clear()
        browser.find_element_by_id("filterText").send_keys("123456789ABCDEFG")

        # Confirm that we are showing no results
        for i in range(60):
            try:
                if browser.find_element_by_id("all_templates_template_no_matches").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        # Use the reset button
        browser.find_element_by_id("reset_button").click()

        # Make sure the no matches message went away
        for i in range(60):
            try:
                if not browser.find_element_by_id("all_templates_template_no_matches").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        # Confirm that we have results
        for i in range(60):
            try:
                if browser.find_element_by_id("all_templates_template_item_line_CEO_Rivals").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

    def is_element_present(self, how, what):
        try: self.browser.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True