from intermine.webservice import Service

service = Service("http://im-dev1.wormbase.org/tools/wormmine/service")


query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryIdentifier", "NOT LIKE", "WBGene*", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #1 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #1 - FAILED')

# ############################################### #

query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("symbol", "IS NULL", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #2 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #2 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryIdentifier", "IS NULL", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #3 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #3 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("chromosome", "IS NULL", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #4 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #4 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "CONTAINS", "2L52.1a", code = "A")

try:
    assert (len(query.rows()) == 1)
    print('Query #5 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #5 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "CONTAINS", "B0207.4", code = "B")
query.add_constraint("symbol", "CONTAINS", "B0207.4", code = "A")
query.set_logic("A or B")

try:
    assert (len(query.rows()) == 1)
    print('Query #6 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #6 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("Allele")
query.add_view("primaryIdentifier", "symbol", "gene.primaryIdentifier",
              "gene.secondaryIdentifier", "gene.symbol")
query.add_constraint("symbol", "=", "gk962622", code = "A")

try:
    assert (len(query.rows()) == 75)
    print('Query #7 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #7 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("secondaryIdentifier", "CONTAINS", "WBGene", code = "A")

try:
    assert (len(query.rows()) == 0)
    print('Query #8 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #8 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("symbol", "CONTAINS", "WBGene", code = "A")

try:
    assert (len(query.rows()) == 0)
    print('Query #9 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #9 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "NOT LIKE", "Transcript:*", code = "A")

try:
    assert (len(query.rows()) == 0)
    print('Query #10 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #10 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "NOT LIKE", "CDS:*", code = "A")

try:
    assert (len(query.rows()) == 0)
    print('Query #11 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #11 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("CDSs", "IS NOT NULL", code = "B")

try:
    assert (len(query.rows()) > 20000)
    print('Query #12 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #12 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol", "sequence.length")
query.add_constraint("symbol", "=", "ZC416.4", code = "A")

for row in query.rows():
    print(row['length'])
    try:
        assert (row['length'] >= 999)
        print('Query #13 Returned correct length - PASSED')
    except:
        print('Query #13 Returned wrong length - FAILED')


# ############################################### #

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "length")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("length", "IS NOT NULL", code = "B")

try:
    assert (len(query.rows()) > 46000)
    print('Query #14 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #14 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism", "IS NULL", code = "A")

try:
    assert (len(query.rows()) == 0)
    print('Query #15 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #15 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("gene", "IS NULL", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #16 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #16 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("gene", "IS NULL", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #17 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #17 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("protein", "IS NULL", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #18 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #18 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("transcripts", "IS NULL", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #19 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #19 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("CDSs", "IS NOT NULL", code = "A")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "B")

try:
    assert (len(query.rows()) > 33000)
    print('Query #20 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #20 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryAccession", "NOT LIKE", "WP:CE*", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #21 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #21 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryIdentifier", "NOT LIKE", "WP:CE*", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #22 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #22 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("CDSs", "IS NULL", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #23 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #23 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("sequence", "IS NULL", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #24 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #24 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism", "IS NULL", code = "A")

try:
    assert (len(query.rows()) == 0)
    print('Query #25 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #25 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism", "IS NULL", code = "A")

try:
    assert (len(query.rows()) == 0)
    print('Query #26 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #26 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_sort_order("Protein.primaryIdentifier", "ASC")
query.add_constraint("primaryAccession", "IS NULL", code = "A")

try:
    assert (len(query.rows()) == 0)
    print('Query #27 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #27 Returned %i - FAILED' % len(query.rows()))


# ############################################### #

query = service.new_query("Allele")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("symbol", "=", "e1370", code = "A")

try:
    assert (len(query.rows()) == 1)
    print('Query #28 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #28 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "LIKE", "CDS:CDS:*", code = "A")

try:
    assert (len(query.rows()) == 0)
    print('Query #29 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #29 Returned %i - FAILED' % len(query.rows()))

# ############################################### #

query = service.new_query("MRNA")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("gene", "IS NULL", code = "B")

try:
    assert (len(query.rows()) == 0)
    print('Query #30 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #30 Returned %i - FAILED' % len(query.rows()))


# # ############################################### #

query = service.new_query("MRNA")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism", "IS NULL", code = "A")

try:
    assert (len(query.rows()) == 0)
    print('Query #31 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #31 Returned %i - FAILED' % len(query.rows()))

# # ############################################### #

query = service.new_query("MRNA")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("CDSs", "IS NULL", code = "A")

try:
    assert (len(query.rows()) == 0)
    print('Query #32 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #32 Returned %i - FAILED' % len(query.rows()))


# # ############################################### #

query = service.new_query("Protein")
query.add_view(
    "primaryAccession", "primaryIdentifier", "CDSs.primaryIdentifier",
    "CDSs.symbol"
)
query.add_sort_order("Protein.primaryIdentifier", "ASC")
query.add_constraint("primaryAccession", "=", "WP:CE46852", code = "A")

try:
    assert (len(query.rows()) == 1)
    print('Query #33 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #33 Returned %i - FAILED' % len(query.rows()))

# # ############################################### #

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol", "protein.primaryAccession",
    "protein.primaryIdentifier")
query.add_constraint("protein.primaryAccession", "=", "WP:CE46852", code = "A")

try:
    assert (len(query.rows()) == 1)
    print('Query #34 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #34 Returned %i - FAILED' % len(query.rows()))


# # ############################################### #

query = service.new_query("Organism")
query.add_view("name", "taxonId")
query.add_constraint("name", "IS NULL", code = "A")

try:
    assert (len(query.rows()) == 0)
    print('Query #35 Returned %i - PASSED' % (len(query.rows())))
except:
    print('Query #35 Returned %i - FAILED' % len(query.rows()))


