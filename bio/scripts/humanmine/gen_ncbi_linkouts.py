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
service = Service("http://humanmine.org/humanmine/service")

# Get a new query on the class (table) you will be querying:
query = service.new_query("Gene")

# The view specifies the output columns
query.add_view("primaryIdentifier", "crossReferences.identifier")

# Uncomment and edit the line below (the default) to select a custom sort order:
# query.add_sort_order("Gene.primaryIdentifier", "ASC")

# You can edit the constraint values below
query.add_constraint("crossReferences.source.name", "=", "NCBI", code = "A")
query.add_constraint("crossReferences.identifier", "!=", "Entrez*", code = "B")
query.add_constraint("organism.name", "=", "Homo sapiens", code = "C")
query.add_constraint("primaryIdentifier", "CONTAINS", "ENSG", code = "D")

############################################################################

prefix = "prid:   9169\n"
prefix = prefix + "dbase:  gene\n"
prefix = prefix + "!base: http://www.humanmine.org/humanmine/portal.do?class=Gene&externalids=\n"

############################################################################

f = open('resources.ft','w')

f.write(prefix)

for row in query.rows():
	f.write("-------------------------------------------------------------------\n")
	f.write("linkid: " + row["primaryIdentifier"] + "\n")
	f.write("uids: " + row["crossReferences.identifier"] + "\n")
	f.write("base: &base.url;\n")
	f.write("rule: " + row["primaryIdentifier"] + "\n")

f.close() 
