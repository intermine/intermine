#!/usr/bin/python

import sys
import urllib2
from xml.sax import make_parser, handler
import time

class SummaryHandler(handler.ContentHandler):

	def __init__(self, output):
		self.counter = 0
		self.content = ''
		self.in_summary = False
		self.gene_id = None

	def startElement(self, name, attrs):
		self.content = ''
		if name == 'Item' and 'Summary' == attrs['Name']:
			self.in_summary = True
		
	def characters(self, content):
		self.content += content

	def endElement(self, name):
		if name == 'DocumentSummary':
			self.counter += 1
		if name == 'uid':
			self.gene_id = self.content
		if self.in_summary:
			if len(self.content.strip()) > 0:
				output.write(self.gene_id + '\t' + self.content + '\n')
			self.in_summary = False
			self.gene_id = None

	def endDocument(self):
		print "fetched info for genes: ", self.counter


def read_gene_ids(gene_info_filename):
	try:
		gene_info = open(gene_info_filename)
	except IOError:
		sys.exit('\nError - failed to open gene info file: ' + gene_info_filename + '\n')
		
	# entrez gene id is second column of file
	gene_ids = [line.split()[1] for line in gene_info]
	gene_ids = filter(lambda name: name.isdigit(), gene_ids)
	return gene_ids

def fetch_summaries(all_gene_ids, output, batch_size):
	parser = make_parser()
	parser.setContentHandler(SummaryHandler(output))
	
	for gene_ids in [all_gene_ids[offset:offset + batch_size] for offset in range(0, len(all_gene_ids), batch_size)]:
                # NCBI requires no more than three requests per second
		time.sleep(1)
		fetch_summary(gene_ids, parser, output)
	

def fetch_summary(gene_ids, parser, output):
	esummary_url = 'https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?tool=intermine&email=bio@flymine.org&db=gene&id='
	id_string = ",".join(gene_ids)
	url = esummary_url + id_string

	print 'Fetching:', url
	parser.parse(urllib2.urlopen(url))
	

if len(sys.argv) != 3:
    print "Usage:"
    print "    ", sys.argv[0], "gene_info_file output_file"
    print "For example:"
    print "    ", sys.argv[0], "/DATA/gene_info_file /DATA/gene_summaries"
    exit(1)

gene_info_filename = sys.argv[1]
gene_ids = read_gene_ids(gene_info_filename)
print 'genes from gene_info file: ', len(gene_ids)

output = open(sys.argv[2], 'w')
fetch_summaries(gene_ids, output, 500)



