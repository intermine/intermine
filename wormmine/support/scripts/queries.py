# 1

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryIdentifier", "NOT LIKE", "WBGene*", code = "B")


# 2

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("symbol", "IS NULL", code = "B")

    
 # 3

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryIdentifier", "IS NULL", code = "B")

    
# 4

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol", "organism.name")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("chromosome", "IS NULL", code = "B")

    
# 5

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "CONTAINS", "2L52.1a", code = "A")

    
# 6

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "CONTAINS", "B0207.4", code = "B")
query.add_constraint("symbol", "CONTAINS", "B0207.4", code = "A")
query.set_logic("A or B")

    
# 7

query = service.new_query("Allele")
query.add_view("primaryIdentifier", "symbol", "gene.primaryIdentifier",
                "gene.secondaryIdentifier", "gene.symbol")
query.add_constraint("symbol", "=", "gk962622", code = "A")


 # 8

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("secondaryIdentifier", "CONTAINS", "WBGene", code = "A")

    
# 9

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("symbol", "CONTAINS", "WBGene", code = "A")


 # 10

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "NOT LIKE", "Transcript:*", code = "A")

    

# 11

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "NOT LIKE", "CDS:*", code = "A")


# 12

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("CDSs", "IS NOT NULL", code = "B")


# 13

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol", "sequence.length")
query.add_constraint("symbol", "=", "ZC416.4", code = "A")
 

# 14

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol", "length")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("length", "IS NOT NULL", code = "B")

    

# 15

query = service.new_query("Gene")
query.add_view("primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism", "IS NULL", code = "A")

    

# 16

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("gene", "IS NULL", code = "B")

    
# 17

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("gene", "IS NULL", code = "B")


# 18

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("protein", "IS NULL", code = "B")


# 19

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("transcripts", "IS NULL", code = "B")


# 20

query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("CDSs", "IS NOT NULL", code = "A")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "B")
 

# 21

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryAccession", "NOT LIKE", "WP:CE*", code = "B")


# 22

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("primaryIdentifier", "NOT LIKE", "WP:CE*", code = "B")


# 23

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("CDSs", "IS NULL", code = "B")



# 24

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("sequence", "IS NULL", code = "B")


# 25

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism", "IS NULL", code = "A")

    
# 26


query = service.new_query("Transcript")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism", "IS NULL", code = "A")


# 27

query = service.new_query("Protein")
query.add_view("primaryAccession", "primaryIdentifier", "secondaryIdentifier", "symbol")
query.add_sort_order("Protein.primaryIdentifier", "ASC")
query.add_constraint("primaryAccession", "IS NULL", code = "A")


# 28

query = service.new_query("Allele")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("symbol", "=", "e1370", code = "A")

    

# 29

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("primaryIdentifier", "LIKE", "CDS:CDS:*", code = "A")

    

# 30

query = service.new_query("MRNA")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism.name", "=", "Caenorhabditis elegans", code = "A")
query.add_constraint("gene", "IS NULL", code = "B")

    
# 31

query = service.new_query("MRNA")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("organism", "IS NULL", code = "A")


# 32

query = service.new_query("MRNA")
query.add_view("primaryIdentifier", "symbol")
query.add_constraint("CDSs", "IS NULL", code = "A")


# 33

query = service.new_query("Protein")
query.add_view(
    "primaryAccession", "primaryIdentifier", "CDSs.primaryIdentifier",
    "CDSs.symbol"
)
query.add_sort_order("Protein.primaryIdentifier", "ASC")
query.add_constraint("primaryAccession", "=", "WP:CE46852", code = "A")


# 34

query = service.new_query("CDS")
query.add_view("primaryIdentifier", "symbol", "protein.primaryAccession",
    "protein.primaryIdentifier")
query.add_constraint("protein.primaryAccession", "=", "WP:CE46852", code = "A")




































