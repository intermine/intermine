import time

from test.querybuildertestcase import QueryBuilderTestCase

class EditTemplateTest(QueryBuilderTestCase):

    def test_edit_template(self):
        self.browser.get(self.base_url + '/template.do?name=ManagerLookup&scope=all')
        self.elem('input.editQueryBuilder').click()
        self.wait().until(lambda d: 'builder' in d.title)
        self.assertIn('Query builder', self.browser.title)
        # Edit the constraint.
        self.elem('img[title="Edit this constraint"]').click()
        con_value = self.wait().until(lambda d: d.find_element_by_css_selector('#attribute8'))
        con_value.clear()
        con_value.send_keys('Anne')
        self.elem('#attributeSubmit').click()
        # Check export.
        self.click_and_wait_for_refresh('a[title="Export this query as XML"]')
        expected_query = '\n'.join([
            '<query name="" model="testmodel" view="Manager.name Manager.title" longDescription="">',
            '  <constraint path="Manager" op="LOOKUP" value="Anne" extraValue=""/>',
            '</query>'])
        self.assertEquals(expected_query, self.elem('body').text)
        self.browser.back()
        # Check results.
        self.run_and_expect(1)
