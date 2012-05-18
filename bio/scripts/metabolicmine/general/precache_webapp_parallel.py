#!/usr/bin/env python

"""
Script to index keyword search and cache region search
A parallel version
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

c1 = pycurl.Curl()
c1.setopt(c1.URL, url + 'keywordSearchResults.do')
c1.setopt(c1.POSTFIELDS, 'searchTerm=FTO&searchSubmit=GO')
c1.setopt(c1.VERBOSE, True)

c2 = pycurl.Curl()
c2.setopt(c2.URL, url + 'genomicRegionSearch.do')
c2.setopt(c2.VERBOSE, True)

m = pycurl.CurlMulti()
m.add_handle(c1)
m.add_handle(c2)
while 1:
    ret, num_handles = m.perform()
    if ret != pycurl.E_CALL_MULTI_PERFORM: break
while num_handles:
    ret = m.select(1.0)
    if ret == -1:  continue
    while 1:
        ret, num_handles = m.perform()
        if ret != pycurl.E_CALL_MULTI_PERFORM: break