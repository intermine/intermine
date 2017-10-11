echo "Extracting exons"
awk '$3 == "exon" {print $0}' $1 > $2
echo "Done"