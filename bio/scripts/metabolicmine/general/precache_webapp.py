#!/usr/bin/env python

"""
Script to index keyword search and cache region search
"""

import sys, pycurl, urllib

DEFAULT_URL = 'http://metabolicmine.org/beta/'

if (len(sys.argv) > 2):
    print 'Usage: python curl_test.py <URL>'
    print 'e.g. python curl_test.py http://metabolicmine.org/beta/'
    sys.exit(1) 
elif (len(sys.argv) == 2):
    try:
        url = sys.argv[1]
        urllib.urlopen(url)
    except IOError:
        print "Not a real URL"
        print 'Usage: curl_test.py <URL>'
        sys.exit(1) 
else:
    print "Use default URL: ", DEFAULT_URL
    url = DEFAULT_URL

url = url if url.endswith("/") else url + "/"

c = pycurl.Curl()
c.setopt(c.VERBOSE, True)
c.setopt(c.FAILONERROR, True)

try:
    c.setopt(c.URL, url + 'keywordSearchResults.do')
    c.setopt(c.POSTFIELDS, 'searchTerm=FTO&searchSubmit=GO')
    c.perform()
    print 'Quick search indexes created.'

    c.setopt(c.URL, url + 'genomicRegionSearch.do')
    print 'Region search cached'

except pycurl.error, error:
    errno, errstr = error
    print 'An error occurred: ', errstr
