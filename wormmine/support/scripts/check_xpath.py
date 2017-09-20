# Paulo Nuin May 2017
# Simple script that checks XPATH listed in the mapping properties file for
# a WormMine against the actual XML.
# As AceDB XMLs don't have a "root" tag, this script wraps the current XML in
# a root tag: i.e. 
# 	<Variations>
# 		<Variation>
# 	...
# 		</Variation>
# 	</Variations>
# Ideally this script should be run on prepped XMLs due to the "open"
# text tags and it should be applied on XMLs ready for build, otherwise
# root tags will break most of the XPATHs in the mapping file

import sys
import os
import re
from lxml import etree

def wrap_xml(xml_file):
	
	xml_contents = open(xml_file).read().splitlines()

	if len(xml_contents[0]) == 0:

		xml_eof = 0
		while xml_eof < len(xml_contents):
			try:
				xml_class = re.search('[A-Z]*>', xml_contents[xml_eof], re.IGNORECASE).group(0)[:-1]
				break
			except:
				xml_eof += 1

		xml_file = open(xml_file, 'r+')
		xml_text = xml_file.read()
		xml_file.seek(0, 0)
		xml_file.write('<' + xml_class + 's>' + xml_text + '</' + xml_class + 's>')
		xml_file.close()
		return xml_class
	else:
		return os.path.basename(xml_file).replace('.xml', '').strip()


def get_mapping_file(mapping_file):

	mapping = open(mapping_file).read().splitlines()

	xpaths = []
	for line in mapping:
		if line.find('=') >= 0 and not line.startswith('#'):
			xpaths.append(line.split('=')[1])
	print xpaths
	return xpaths


def check_xpath(xml_file, xpaths, xml_class):


	print 'Analysing '  + xml_file
	xml = etree.parse(xml_file)
	print 'done'
	for i in xpaths:
		xpath = '/' + xml_class + i.strip()
		print xpath
		run = xml.xpath(xpath)
		print 'there are %i items with the above XPATH in the XML' % (len(run))
		print run[0:500]
		print
		print

if __name__ == '__main__':

	xml_file = sys.argv[1]
	mapping_file = sys.argv[2]
	# xml_class = wrap_xml(xml_file) + 's'
	xml_class = 'Variations'
	xpaths = get_mapping_file(mapping_file)
	check_xpath(sys.argv[1], xpaths, xml_class)
