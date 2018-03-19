echo "Extracting mRNAs"
awk '$3 == "mRNA" {print $0}' $1 > $2
echo "Done"