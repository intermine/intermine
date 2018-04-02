from intermine.webservice import Service

service = Service("http://intermine.wormbase.org/tools/wormmine/service")


print('Query #1')

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryIdentifier", "NOT LIKE", "WBGene*", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #2')
print('failed')

#query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "organism.name")
#query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
#query.add_constraint("symbol", "IS NULL", code = "B")

#assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #3')


query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryIdentifier", "IS NULL", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #4')
print('failed')

#query = service.new_query("Transcript")
#query.add_view("primaryIdentifier", "symbol", "organism.name")
#query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
#query.add_constraint("chromosome", "IS NULL", code = "B")

#assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #5')

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "CONTAINS", "2L52.1a", code = "A")

assert (len(query.rows()) == 1), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #6')

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "CONTAINS", "B0207.4", code = "B")
query.add_constraint("symbol", "CONTAINS", "B0207.4", code = "A")
query.set_logic("A or B")

assert (len(query.rows()) == 1), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #7')
print('failed')

#query = service.new_query("Allele")
#query.add_view("primaryIdentifier", "symbol", "gene.primaryIdentifier",
#               "gene.secondaryIdentifier", "gene.symbol")
#query.add_constraint("symbol", "=", "gk962622", code = "A")

#assert (len(query.rows()) == 75), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #8')

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("secondaryIdentifier", "CONTAINS", "WBGene", code = "A")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #9')

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("symbol", "CONTAINS", "WBGene", code = "A")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #10')

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "NOT LIKE", "Transcript:*", code = "A")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #11')

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "NOT LIKE", "CDS:*", code = "A")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #12')

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("CDSs", "IS NOT NULL", code = "B")

assert (len(query.rows()) > 20000), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #13')
print('failed')

#query = service.new_query("CDS")
#query.add_view("primaryIdentifier", "symbol", "sequence.length")
#query.add_constraint("symbol", "=", "ZC416.4", code = "A")

#for row in query.rows():
#    assert (row['length'] > 999), 'Wrong CDS length'

# ############################################### #
print('Query #14')

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "length")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("length", "IS NOT NULL", code = "B")

assert (len(query.rows()) > 46000), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #15')

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism", "IS NULL", code = "A")

#assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #16')

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("gene", "IS NULL", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #17')

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("gene", "IS NULL", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #18')

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("protein", "IS NULL", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #19')

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("transcripts", "IS NULL", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #20')

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("CDSs", "IS NOT NULL", code = "A")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "B")

assert (len(query.rows()) > 33000), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #21')

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryAccession", "NOT LIKE", "WP:CE*", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #22')

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryIdentifier", "NOT LIKE", "WP:CE*", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #23')

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("CDSs", "IS NULL", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #24')

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("sequence", "IS NULL", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #25')
print('failed')
query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism", "IS NULL", code = "A")

#assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #26')

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism", "IS NULL", code = "A")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #27')
print('failed')
query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_sort_order("Protein.primaryIdentifier", "ASC")
query.add_constraint("primaryAccession", "IS NULL", code = "A")

#assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# ############################################### #
print('Query #28')
print('failed')
query = service.new_query("Allele")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("symbol", "=", "e1370", code = "A")

#assert (len(query.rows()) == 1), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #29')

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "LIKE", "CDS:CDS:*", code = "A")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# ############################################### #
print('Query #30')

query = service.new_query("MRNA")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("gene", "IS NULL", code = "B")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# # ############################################### #
print('Query #31')

query = service.new_query("MRNA")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism", "IS NULL", code = "A")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))

# # ############################################### #
print('Query #32')

query = service.new_query("MRNA")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("CDSs", "IS NULL", code = "A")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))


# # ############################################### #
print('Query #33')

query = service.new_query("Protein")
query.add_view(
    "primaryAccession", "primaryIdentifier", "CDSs.primaryIdentifier",
    "CDSs.symbol"
)
query.add_sort_order("Protein.primaryIdentifier", "ASC")
query.add_constraint("primaryAccession", "=", "WP:CE46852", code = "A")

assert (len(query.rows()) == 1), 'Returned %i' % (len(query.rows()))

# # ############################################### #
print('Query #34')

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol", "protein.primaryAccession",
    "protein.primaryIdentifier")
query.add_constraint("protein.primaryAccession", "=", "WP:CE46852", code = "A")

assert (len(query.rows()) == 1), 'Returned %i' % (len(query.rows()))


# # ############################################### #
print('Query #35')

query = service.new_query("Organism")
query.add_view("name", "taxonId")
query.add_constraint("name", "IS NULL", code = "A")

assert (len(query.rows()) == 0), 'Returned %i' % (len(query.rows()))




