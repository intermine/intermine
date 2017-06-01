# Paulo Nuin May 2017
# Simple script that extracts the XPATHs from a wrapped XML file
# A wrapper might need to be run on the XML file prior to run this script,
# and ideally all txt entries should be escaped (TODO)
# Items and fields with more than one [] entry are not included in the output
# the same way text() are not output. This script can be used in combination with
# check_xpaths.py


import sys
import os
from lxml import etree
import re


def extract_xpaths(xml_file):

	root = etree.fromstring(open(xml_file).read())
	tree = etree.ElementTree(root)


	xpaths = []
	for node in tree.iter():
		xpath = tree.getpath(node)
		new_xpath = re.sub(r'\[[0-9]*\]', '', xpath)
		xpaths.append(new_xpath) 

	uxpaths = []
	[uxpaths.append(item) for item in xpaths if item not in uxpaths]
	print len(uxpaths)
	return uxpaths


if __name__ == '__main__':

	xml_file = sys.argv[1]
	# xml_file = '../../../../WS259-test-data/Expr_pattern.xml'
	for i in extract_xpaths(xml_file):
		print i
