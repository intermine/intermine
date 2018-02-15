# Paulo Nuin November 2017

import sys
import os
import re
from lxml import etree

def extract_elements(xml_file):

    xml_contents = open(xml_file).read()
    root = etree.fromstring(xml_contents)
    print root.tag



#    for tags in root.iter('b'):         # root is the ElementTree object
# ...     print tags.tag, tags.text


    # if len(xml_contents[0]) == 0:

    #     xml_eof = 0
    #     while xml_eof < len(xml_contents):
    #         try:
    #             xml_class = re.search('[A-Z]*>', xml_contents[xml_eof], re.IGNORECASE).group(0)[:-1]
    #             break
    #         except:
    #             xml_eof += 1

    #     xml_file = open(xml_file, 'r+')
    #     xml_text = xml_file.read()
    #     xml_file.seek(0, 0)
    #     xml_file.write('<' + xml_class + 's>' + xml_text + '</' + xml_class + 's>')
    #     xml_file.close()
    #     return xml_class
    # else:
    #     return os.path.basename(xml_file).replace('.xml', '').strip()


if __name__ == '__main__':

    xml_file = sys.argv[1]
    extract_elements(xml_file)