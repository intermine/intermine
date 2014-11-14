import time

from selenium.webdriver.support.ui import Select, WebDriverWait

from test.querybuildertestcase import QueryBuilderTestCase as Super

EXPECTED_TYPES = ['Bank', 'Broke', 'Employment Period', 'Has Address',
    'Has Secretarys', 'Important Person', 'Random Interface', 'Range', 'Secretary', 'Thing', 'Types']

class StartQueryFromSelect(Super):

    def test_start_query_from_select(self):
        cls = 'Employee'
        Select(self.elem("#queryClassSelector")).select_by_visible_text(cls)
        self.elem("#submitClassSelect").click()
        time.sleep(3)
        self.assertIn('Query builder', self.browser.title)
        self.assertEquals(cls, self.elem('.typeSelected').text)
        self.elem('a[title="Show Employee in results"] > img.arrow').click()

