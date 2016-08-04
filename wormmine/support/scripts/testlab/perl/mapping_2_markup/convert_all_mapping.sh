mapping_files='/nfs/wormbase/wormmine/website-intermine/acedb-dev/intermine/datadir/wormbase-acedb/*/mapping/*.properties'

for f in $mapping_files
do
	echo "Processing $f"
	new_f=$f".markup"
	perl mapping_2_markup.pl $f $new_f
	less $new_f
	rm $new_f

done
