#!/usr/bin/env python

# This is an automatically generated script to run your query
# to use it you will require the intermine python client.
# To install the client, run the following command from a terminal:
#
#     sudo easy_install intermine
#
# For further documentation you can visit:
#     http://intermine.readthedocs.org/en/latest/web-services/

# The following two lines will be needed in every python script:
from intermine.webservice import Service
service = Service("http://human.intermine.org/human/service")

# Get a new query on the class (table) you will be querying:
query = service.new_query("Gene")

# The view specifies the output columns
query.add_view("primaryIdentifier")

# Uncomment and edit the line below (the default) to select a custom sort order:
#query.add_sort_order("Pathway.primaryIdentifier", "ASC")

# You can edit the constraint values below
query.add_constraint("primaryIdentifier", "IS NOT NULL", code = "A")
query.add_constraint("organism.shortName", "=", "H. sapiens", code = "B")

# Uncomment and edit the code below to specify your own custom logic:
# query.set_logic("A")

sitemapCount = 0;

prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
prefix = prefix + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n" 
prefix = prefix + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" 
prefix = prefix + "  xsi:schemaLocation=\"http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd\">\n" 

postfix = "</urlset>"

rowCount = 0

f = open('sitemap' + str(sitemapCount) + ".xml",'w')

f.write(prefix)

for row in query.rows():
	f.write("<url><loc>http://www.humanmine.org/human/portal.do?class=Gene&amp;externalids=" + row["primaryIdentifier"] + "</loc></url>\n")
	rowCount = rowCount + 1
	if rowCount >= 50000:	
		f.write(postfix)
		f.close() 
		sitemapCount = sitemapCount + 1
		f = open('sitemap' + str(sitemapCount) + ".xml",'w')
		f.write(prefix)	
		rowCount = 1
f.write(postfix)
f.close() 
