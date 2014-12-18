import time

from test.querybuildertestcase import QueryBuilderTestCase

class LoginToViewSavedTest(QueryBuilderTestCase):

    def test_on_right_page(self):
        self.wait().until(lambda b: 'query' in b.title)
        self.assertIn('Custom query', self.browser.title)

    def test_login_to_view_saved(self):
        link = self.findLink("Login to view saved queries")
        self.assertIsNotNone(link)
