#!/usr/bin/python

import sys
import urllib
from datetime import datetime

#http://intermine.modencode.org/release-16/features.do?type=experiment&action=export&experiment=Lieb%20Chromatin%20Function%20Elements%20ChIP-chip&feature=BindingSite&format=gff3
#base_url = "http://intermine.modencode.org/release-16/features.do?type=experiment&action=export&experiment="
#base_url = "http://mod2:8080/modminepre-rns/features.do?type=experiment&action=export&format=gff3&experiment="
site = "http://intermine.modencode.org/"
start_url = "/features.do?type=experiment&action=export&format=gff3&experiment="

def make_url(release, exp, feature, gzip):
	url = site + release + start_url + urllib.quote(exp) + "&feature=" + feature
	if gzip:
		url = url + "&gzip=true"
	return url

def make_filename(org, exp, feature, gzip):
	filename = org.replace('.', '') + "_" + exp + "_" + feature + ".gff3"
	filename = filename.replace(' ', '_')
	if gzip:
		filename = filename + ".gz"
	return filename

def file_len(filename):
	f = open(filename)
	nr_of_lines = sum(1 for line in f)
	f.close()
	return nr_of_lines


if len(sys.argv) not in (3,4):
	print "Usage: " + sys.argv[0] + " feature_table_file release [gzip]"
	exit(1)

feature_table_file = sys.argv[1]
feature_table = open(feature_table_file)

release = sys.argv[2]

gzip = False
if len(sys.argv) == 4 and sys.argv[3] == 'gzip':
	gzip = True
	
line_num = 0
for line in feature_table:
	line_num += 1
	if (line_num < 3):
		continue

	if '|' in line:
		(org, exp, feature, count) = line.split('|')
	
		exp = exp.strip()
		org = org.strip()
		
		feature = feature.split('.')[-1].strip()

		print "\n", org, exp, feature, count
		url = make_url(release, exp, feature, gzip)
		local_file = make_filename(org, exp, feature, gzip)
		print "fetching " + url + "\n\t- to file: " + local_file
		start_time = datetime.now()
		urllib.urlretrieve(url, local_file)

		if not gzip:
			file_length = file_len(local_file)
			expected_file_length = int(count) + 1
			if file_length != expected_file_length:
				print "\t- WARNING: expected file length of:", expected_file_length, "but was: ", file_length,  ".  This may indicate features without locations"
			else:
				print "\t- SUCCESS"
	
		print "\t- took: ", (datetime.now() - start_time)
		
feature_table.close()