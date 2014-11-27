import time
from test.querybuildertestcase import QueryBuilderTestCase

EXPECTED_TYPES = ['Bank', 'Broke', 'Employment Period', 'Has Address',
    'Has Secretarys', 'Important Person', 'Random Interface', 'Range', 'Secretary', 'Thing', 'Types']

class BrowseDataModelTest(QueryBuilderTestCase):

    def test_browse_data_model(self):
        link = self.findLink("Browse data model")
        self.assertIsNotNone(link)
        link.click()
        help_text = self.elem('.body > p').text
        self.assertIn("browse the tree", help_text)
        for type_name in EXPECTED_TYPES:
            self.assertIsNotNone(self.findLink(type_name))

        self.findLink('Bank').click()
        time.sleep(3)
        self.assertIn('Query builder', self.browser.title)
        self.assertEquals('Bank', self.elem('.typeSelected').text)
