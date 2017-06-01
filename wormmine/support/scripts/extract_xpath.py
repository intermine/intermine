# Paulo Nuin May 2017
# Simple script that 


import sys
import os
from lxml import etree



def extract_xpaths(xml_file):

	tree = etree.parse(xml_file)

	root = tree.


	for node in tree.iter():
		print etree.getpath(node)
		# for child in node.getchildren():
		# 	print node.tag, child.tag


#     
#          if child.text.strip():
#             print("{}.{} = {}".format(root, ".".join(tree.getelementpath(child).split("/")), child.text.strip()))




if __name__ == '__main__':

	xml_file = sys.argv[1]
	extract_xpaths(xml_file)