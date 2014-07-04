from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
from selenium.common.exceptions import NoSuchElementException
from test.testmodeltestcase import TestModelTestCase as Super
import unittest, time, re
from imuser import IMUser

class TemplatesMyStarTest(Super):

    def setUp(self):
        Super.setUp(self)
        self.user = IMUser("zombie-testing-account-login@intermine.org");

    def test_templates_my_star(self):

        browser = self.browser
        browser.get(self.base_url + "/login.do")
        browser.find_element_by_name("username").clear()
        browser.find_element_by_name("username").send_keys(self.user.name)
        browser.find_element_by_name("password").clear()
        browser.find_element_by_name("password").send_keys(self.user.password)
        browser.find_element_by_name("action").click()

        # Create a template
        browser.get(self.base_url + "/import.do")

        custom_template = """
            <template name="My_Template_1" title="My Template 1" comment="">
              <query name="CEO_Rivals" model="testmodel" view="CEO.name CEO.salary CEO.seniority CEO.company.name" longDescription="">
                <constraint path="CEO.name" editable="true" op="!=" value="EmployeeB1"/>
              </query>
            </template>"""

        browser.find_element_by_id("xml").send_keys(custom_template)
        browser.find_element_by_css_selector("div > input[type=\"submit\"]").click()

        browser.get(self.base_url + "/templates.do")

        # Confirm that we have two visible non favorite templates
        self.assert_visible_id("all_templates_template_item_line_Underwater_People")
        self.assert_visible_id("all_templates_template_item_line_CEO_Rivals")

        # Add a template to our favorites
        browser.find_element_by_id("favourite_ManagerLookup").click()

        # Show only our favorites
        browser.find_element_by_id("filter_favourites_all_templates_template").click()

        # Confirm that our non-favorite templates are gone
        self.assert_invisible_id("all_templates_template_item_line_Underwater_People")
        self.assert_invisible_id("all_templates_template_item_line_CEO_Rivals")

        # Confirm that our favorite templates is visible
        self.assert_visible_id("all_templates_template_item_line_ManagerLookup")

        # Turn off the favorites filter
        browser.find_element_by_id("filter_favourites_all_templates_template").click()

        # Show only MY filters
        browser.find_element_by_id("filter_scope_all_templates_template").click()

        # Confirm that we see our template
        self.assert_visible_id("all_templates_template_item_line_My_Template_1")

        # Confirm that we don't see others
        self.assert_invisible_id("all_templates_template_item_line_Underwater_People")
        self.assert_invisible_id("all_templates_template_item_line_CEO_Rivals")
        self.assert_invisible_id("all_templates_template_item_line_ManagerLookup")

    def assert_visible_id(self, id):
        browser = self.browser
        for i in range(60):
            try:
                if browser.find_element_by_id(id).is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")
        return True

    def assert_invisible_id(self, id):
        browser = self.browser
        for i in range(60):
            try:
                if not browser.find_element_by_id(id).is_displayed(): break
            except: pass
            time.sleep(1)
        else: self.fail("time out")
        return True

    def is_element_present(self, how, what):
        try: self.browser.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
    
    def tearDown(self):
        self.user.delete()