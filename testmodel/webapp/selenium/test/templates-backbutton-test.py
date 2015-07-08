from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re, os

UNDERWATER = '#all_templates_template_item_line_Underwater_People'
RIVALS = '#all_templates_template_item_line_CEO_Rivals'

class TemplatesBackButton(Super):

    def setUp(self):
        Super.setUp(self)

    def test_templates_backbutton(self):

        browser = self.browser
        browser.get(self.base_url + "/templates.do")
        browser.find_element_by_id("filterText").clear()
        browser.find_element_by_id("filterText").send_keys("underwater")

        rivals = self.wait_for_elem(RIVALS)
        underwater = self.wait_for_elem(UNDERWATER)

        self.wait().until(lambda d: not rivals.is_displayed())
        self.wait().until(lambda d: underwater.is_displayed())

        # Click the link to underwater people
        self.click_and_wait_for_refresh(browser.find_element_by_link_text("Underwater People"))

        # Go back one in the history
        browser.back()

        rivals_2     = self.wait_for_elem(RIVALS)
        underwater_2 = self.wait_for_elem(UNDERWATER)

        # Assert that our filter still has the value of "underwater"
        self.assertEqual("underwater", browser.find_element_by_id("filterText").get_attribute("value"))

        self.wait().until(lambda d: not rivals_2.is_displayed())
        self.wait().until(lambda d: underwater_2.is_displayed())

