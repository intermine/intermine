echo "Scraping GFF3"

egrep "^\S*\s(WormBase)\S*\s(gene|mRNA|CDS|exon)" $1 > $2