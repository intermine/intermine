from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re, os

UNDERWATER = '#all_templates_template_item_line_Underwater_People'
MANAGERS = '#all_templates_template_item_line_ManagerLookup'
EMPLOYEES = '#all_templates_template_item_line_employeeByName'

class TemplatesFilterAspect(Super):
    def setUp(self):
        Super.setUp(self)

    def test_templates_filter_aspect(self):

        browser = self.browser
        browser.get(self.base_url + "/templates.do")
        filter_text = self.elem('#filterText')
        filter_text.clear()

        # Select People for our aspect
        Select(browser.find_element_by_id("all_templates_template_filter_aspect")).select_by_visible_text("People")

        underwater = self.elem(UNDERWATER)
        managers = self.elem(MANAGERS)
        employees = self.elem(EMPLOYEES)

        # Confirm that "Underwater People" went away (which doesn't actually contain people)
        self.wait().until(lambda d: not underwater.is_displayed())
        # Confirm that employees are still there.
        self.wait().until(lambda d: employees.is_displayed())

        # Filter for CEO templates
        filter_text.send_keys("managers")

        # Confirm that we lost employees by name
        self.wait().until(lambda d: not employees.is_displayed())
        # Confirm that we have a managers template
        self.wait().until(lambda d: managers.is_displayed())
