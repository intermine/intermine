import unittest
import requests
import os

primaryId = 'PFL1385c'
query = """
{
    "regions": ["MAL12:1160000..1163000"],
    "featureTypes": ["Gene", "Exon", "Intron"],
    "organism": "P. falciparum 3D7"
}
"""

URL = os.getenv('BIO_TEST_URL', 'http://localhost:8080/biotestmine')

class TestIssue728(unittest.TestCase):

    def testRegionsSequence(self):
        resource = URL + '/service/regions/sequence'
        self.assertResourceWorks(resource)

    def testRegionsFasta(self):
        resource = URL + '/service/regions/fasta'
        self.assertResourceWorks(resource)

    def assertResourceWorks(self, resource):
        resp = requests.get(resource, params = {'query': query})
        print(resp.text)
        self.assertIn(primaryId, resp.text)
        self.assertEqual(200, resp.status_code)

