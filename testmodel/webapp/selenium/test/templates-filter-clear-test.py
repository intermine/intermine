from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re, os

RIVALS = '#all_templates_template_item_line_CEO_Rivals'
NO_MATCHES = '#all_templates_template_no_matches'

class TemplatesFilterClear(Super):
    def setUp(self):
        Super.setUp(self)

    def test_templates_filter_clear(self):

        browser = self.browser
        browser.get(self.base_url + "/templates.do")
        filter_text = self.elem('#filterText')
        filter_text.clear()

        rivals = self.elem(RIVALS)
        no_matches = self.elem(NO_MATCHES)

        # Confirm that we have results
        self.wait().until(lambda d: rivals.is_displayed())

        # Make all elements go away
        filter_text.clear()
        filter_text.send_keys("123456789ABCDEFG")

        # Confirm that we are showing no results
        self.wait().until(lambda d: not rivals.is_displayed())
        self.wait().until(lambda d: no_matches.is_displayed())

        # Use the reset button
        self.elem("#reset_button").click()

        # Make sure the no matches message went away
        self.wait().until(lambda d: not no_matches.is_displayed())

        # Confirm that we have results
        self.wait().until(lambda d: rivals.is_displayed())
