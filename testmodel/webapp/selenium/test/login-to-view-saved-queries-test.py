import time

from test.querybuildertestcase import QueryBuilderTestCase

class LoginToViewSavedTest(QueryBuilderTestCase):

    def test_on_right_page(self):
        time.sleep(3)
        self.assertIn('Custom query', self.browser.title)

    def test_login_to_view_saved(self):
        link = self.findLink("Login to view saved queries")
        self.assertIsNotNone(link)
