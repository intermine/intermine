echo "Extracting genes"
awk '$3 == "gene" {print $0}' $1 > $2
echo "Done"
