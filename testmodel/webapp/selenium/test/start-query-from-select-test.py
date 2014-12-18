import time

from selenium.webdriver.support.ui import Select

from test.querybuildertestcase import QueryBuilderTestCase as Super

EXPECTED_TYPES = ['Bank', 'Broke', 'Employment Period', 'Has Address',
    'Has Secretarys', 'Important Person', 'Random Interface', 'Range', 'Secretary', 'Thing', 'Types']

class StartQueryFromSelect(Super):

    def test_start_query_from_select(self):
        cls = 'Employee'
        Select(self.elem("#queryClassSelector")).select_by_visible_text(cls)
        self.elem("#submitClassSelect").click()
        self.wait().until(lambda d: 'builder' in d.title)
        self.assertIn('Query builder', self.browser.title)
        self.assertEquals(cls, self.elem('.typeSelected').text)
        self.elem('a[title="Show Employee in results"] > img.arrow').click()

