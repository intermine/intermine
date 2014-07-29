from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re, os

class TemplatesFilterAspect(Super):
    def setUp(self):
        Super.setUp(self)
    
    def test_templates_filter_aspect(self):

        browser = self.browser
        browser.get(self.base_url + "/templates.do")
        browser.find_element_by_id("filterText").clear()

        # Select People for our aspect
        Select(browser.find_element_by_id("all_templates_template_filter_aspect")).select_by_visible_text("People")

        # Confirm that "Underwater People" went away (which doesn't actually contain people)
        for i in range(60):
            try:
                if not browser.find_element_by_id("all_templates_template_item_line_Underwater_People").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        # Confirm that we still have expected results
        for i in range(60):
            try:
                if browser.find_element_by_id("all_templates_template_item_line_employeeByName").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        # Confirm that we still have expected results
        for i in range(60):
            try:
                if browser.find_element_by_id("all_templates_template_item_line_employeeByName").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        # Filter for CEO templates
        browser.find_element_by_id("filterText").send_keys("managers")

        # Confirm that we have a managers template
        for i in range(60):
            try:
                if browser.find_element_by_id("all_templates_template_item_line_ManagerLookup").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

        # Confirm that we lost employees by name
        for i in range(60):
            try:
                if not browser.find_element_by_id("all_templates_template_item_line_employeeByName").is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")

    def is_element_present(self, how, what):
        try: self.browser.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True