from intermine.webservice import Service

service = Service("http://im-dev1.wormbase.org/tools/wormmine/service")



print('Query #1')

# 1 Query for C. elegans genes in which the WormBase Gene ID (primaryidentifier) 
# is not like WBGene should return 0 results

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryIdentifier", "NOT LIKE", "WBGene*", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #2')

# Query for C. elegans genes in which there is no Gene Name (public_name); should return 0 results:

# query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "organism.name")
# query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
# query.add_constraint("symbol", "IS NULL", code = "B")

# assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #3')

# Query for C. elegans genes where WormBase Gene ID (primaryidentifier) 
# is null/empty; should return 0 results:

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryIdentifier", "IS NULL", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #4')

# Query for all C. elegans transcripts that don't have a chromosome; should return 0 results

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("chromosome", "IS NULL", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
# ############################################### #
print('Query #')
