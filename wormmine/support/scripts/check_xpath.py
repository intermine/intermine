# Paulo Nuin May 2017
# 

import sys
import os
import re
from lxml import etree


def wrap_xml(xml_file):
	
	xml_contents = open(xml_file).read().splitlines()
	if xml_contents[0] == '':
		xml_class = re.search('[A-Z]*>', xml_contents[1], re.IGNORECASE).group(0)[:-1]

		xml_file = open(xml_file, 'r+')
		xml_text = xml_file.read()
		xml_file.seek(0, 0)
		xml_file.write('<' + xml_class + 's>' + xml_text + '</' + xml_class + 's>')
		xml_file.close()
		return xml_class
	else:
		return os.path.basename(xml_file).replace('.xml', '').strip()
		return 'already wrapped'

def get_mapping_file(mapping_file):

	mapping = open(mapping_file).read().splitlines()

	xpaths = []
	for line in mapping:
		if line.find('=') >= 0:
			xpaths.append(line.split('=')[1])

	return xpaths


def check_xpath(xml_file, xpaths, xml_class):


	xml = etree.parse(xml_file)
	for i in xpaths:
		xpath = '/' + xml_class + i.strip()
		run = xml.xpath(xpath)
		print len(run)


if __name__ == '__main__':

	xml_file = sys.argv[1]
	mapping_file = sys.argv[2]
	xml_class = wrap_xml(xml_file) + 's'
	xpaths = get_mapping_file(mapping_file)
	check_xpath(sys.argv[1], xpaths, xml_class)
