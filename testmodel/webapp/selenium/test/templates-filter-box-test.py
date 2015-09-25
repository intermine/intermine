from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re, os

RIVALS = '#all_templates_template_item_line_CEO_Rivals'
MANAGERS = '#all_templates_template_item_line_ManagerLookup'
UNDERWATER = '#all_templates_template_item_score_Underwater_People'

class TemplatesFilterBox(Super):
    def setUp(self):
        Super.setUp(self)

    def test_templates_filter_box(self):

        browser = self.browser
        browser.get(self.base_url + "/templates.do")
        browser.find_element_by_id("filterText").clear()

        managers = self.elem(MANAGERS)
        rivals = self.elem(RIVALS)
        underwater = self.elem(UNDERWATER)

        # Filter for CEO templates
        browser.find_element_by_id("filterText").send_keys("CEO")

        # Confirm that we still have our CEO template
        self.wait().until(lambda d: rivals.is_displayed())

        # Confirm that a non-CEO template disappeared
        self.wait().until(lambda d: not managers.is_displayed())

        # Filter by "Underwater"
        browser.find_element_by_id("filterText").clear()
        browser.find_element_by_id("filterText").send_keys("Underwater")

        # Confirm that we still have our Underwater template
        self.wait().until(lambda d: underwater.is_displayed())

        # Confirm that a CEO went away
        self.wait().until(lambda d: not rivals.is_displayed())

