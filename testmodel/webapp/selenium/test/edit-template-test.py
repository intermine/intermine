import time

from test.querybuildertestcase import QueryBuilderTestCase

class EditTemplateTest(QueryBuilderTestCase):

    def test_edit_template(self):
        self.browser.get(self.base_url + '/template.do?name=ManagerLookup&scope=all')
        self.elem('input.editQueryBuilder').click()
        time.sleep(3)
        self.assertIn('Query builder', self.browser.title)
        # Edit the constraint.
        self.elem('img[title="Edit this constraint"]').click()
        con_value = self.elem('#attribute8')
        con_value.clear()
        con_value.send_keys('Anne')
        self.elem('#attributeSubmit').click()
        # Check export.
        self.elem('a[title="Export this query as XML"]').click()
        expected_query = '\n'.join([
            '<query name="" model="testmodel" view="Manager.name Manager.title" longDescription="">',
            '  <constraint path="Manager" op="LOOKUP" value="Anne" extraValue=""/>',
            '</query>'])
        self.assertEquals(expected_query, self.elem('body').text)
        self.browser.back()
        # Check results.
        self.elem('#showResult').click()
        self.assertRowCountIs(1)
