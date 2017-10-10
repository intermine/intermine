echo "Extracting CDSs"
awk '$3 == "CDS" {print $0}' $1 > $2
echo "Done"
