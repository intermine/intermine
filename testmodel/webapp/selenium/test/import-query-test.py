from test.querybuildertestcase import QueryBuilderTestCase

import time

from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

class ImportQueryTest(QueryBuilderTestCase):

    def test_import_query(self):
        link = self.findLink("Import query from XML")
        self.assertIsNotNone(link)
        link.click()
        time.sleep(3)
        self.assertIn('Import Query', self.browser.title)
        input_box = self.elem('#xml')
        self.assertIsNotNone(input_box)
        query = ''.join([
            '<query model="testmodel" view="Bank.debtors.debt" sortOrder="Bank.debtors.debt asc">',
            '<constraint path="Bank.debtors.debt" op="&gt;" value="1000"/>',
            '</query>'
            ])

        input_box.send_keys(query)
        self.assertEquals('true', self.elem('#file').get_attribute('disabled'))
        self.elem('#importQueriesForm input[type="submit"]').click()
        time.sleep(3)

        self.assertEquals('Bank', self.elem('.typeSelected').text)
        constraints = self.elems('span.constraint')
        self.assertEquals(1, len(constraints))
        self.assertEquals('> 1000', constraints[0].text)
        self.assertEquals(1, len(self.browser.find_elements_by_class_name('viewpath')))

