#!/usr/bin/env python

"""
Script to fetch pubmed ids from mim number by using OMIM API

Prerequisite:
Requests: HTTP for Humans
	URL: http://docs.python-requests.org/en/latest/
	Installation: sudo easy_install requests

Notice:
	To run the script under /micklem/data/metabolic/omim/script    
"""

import os, json, requests, time, datetime, types

## TODO use a better logging system

API_KEY = 'F5DE0CBD69F9F6771E2E0D3BEFD9E7A1D4C3A994' # the API key will expire after one year from the issue date
# estimated expiry - Jan 6th 2015
# http://api.omim.org/api/html/apiKey.html
OMIM_SERVICE_BASE_USA = 'http://api.omim.org/api/entry/referenceList'
# http://api.europe.omim.org/api/html/apiKey.html
OMIM_SERVICE_BASE_EUROPE = 'http://api.europe.omim.org/api/entry/referenceList'

LOG_DIR = '/tmp/'
LOG_NAME = 'pubmed_cited.log'
MIM_NUMBER_FILE = '/micklem/data/metabolic/omim/current/mim2gene.txt'
PUBMED_CITED_FILE = '/micklem/data/metabolic/omim/current/pubmed_cited'

class OMIMQueryError(Exception):
	pass

def get_omim_pubmed(mimNumber, log):
	log.write('Parsing MIM[' + mimNumber + ']\n')

	params = dict(
		mimNumber = mimNumber,
		apiKey = API_KEY,
		format = 'json'
	)

	resp = requests.get(url=OMIM_SERVICE_BASE_EUROPE, params=params)
	data = resp.json() # In case the JSON decoding fails, r.json simply returns None.

	log.write('HTTP response in JSON: ' + str(data) + '\n')

	## parse pubmedId in JSON string
	pubmed_cited_list = list()
	ref_list = list()
	if type(data) is not types.NoneType:
		ref_lists = data['omim']['referenceLists']
		for ref_list in ref_lists:
			pubmedID_count = 0
			for ref in ref_list['referenceList']:
				try:
					pubmedID = ref['reference']['pubmedID']
					pubmedID_count += 1
					pubmed_cited_list.append(str(mimNumber) + '\t' + str(pubmedID_count) + '\t' + str(pubmedID))
				except KeyError, e:
					print 'MIM[' + mimNumber + '] pubmedID does not exist:', ref['reference']
					log.write('MIM[' + mimNumber + '] pubmedID does not exist: ' + str(ref['reference']) + '\n')

	return pubmed_cited_list

def parse_mim_number(mim_number_file):
	mim_number_set = set() # mim in mim2gene are already unique, can replace set to list to main order, easy to validate by observation
	f = open(mim_number_file, 'r')

	for line in f:
		if not line.startswith('#'):
			mim_number_set.add(line.split('\t').pop(0))

	return mim_number_set

def timestamp_file(fname, fmt='%Y-%m-%d-%H-%M-%S_{fname}'):
	return datetime.datetime.now().strftime(fmt).format(fname=fname)

def main():
	## parse MIM number from mim2gene.txt file
	try:
		mim_number_set = parse_mim_number(MIM_NUMBER_FILE)
	except:
		print "Error while parsing mim2gene.txt"

	## get log file
	log = open(LOG_DIR + timestamp_file(LOG_NAME),'w+')
	pubmed_cited_file = open(PUBMED_CITED_FILE,'w')

	## send http requests to OMIM API (limit 4 requests/sec)		
	for mim_number in mim_number_set:
		try:
			pubmed_cited_list = get_omim_pubmed(mim_number, log)
			pubmed_cited_file.write('\n'.join(pubmed_cited_list) + '\n')
			time.sleep(1) # thread sleeps in sec
		except OMIMQueryError, e:
			print "An API error occurred."

	log.close()
	pubmed_cited_file.close()
		
if __name__ == "__main__":
    main()
