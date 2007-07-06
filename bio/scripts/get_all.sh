#!/bin/sh
logdir="/shared/data/download_logs/"
if [ ! -d $logdir ]
then
	mkdir -p $logdir
fi
tempname="temp_log.txt"
tempfile=$logdir$tempname
shared_data="/shared/data"
config_file="./resources/get_scripts.config"
rm $tempfile
echo "==========================================================="
echo "Getting Fly Anatomy Ontology"
./get_fly_anatomy_ontology $logdir $tempname $shared_data
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting FlyAtlas"
./get_flyatlas $logdir $tempname $shared_data
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting go annotation"
./get_go-annotation $logdir $tempname $shared_data $config_file
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting intact data"
./get_intact $logdir $tempname $shared_data $config_file
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting KEGG data"
./get_kegg $logdir $tempname $shared_data
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting wormbase identifiers"
./get_wormbase_identifiers $logdir $tempname $shared_data
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting wormbase rnai"
./get_wormbase_rnai $logdir $tempname $shared_data
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting pubmed data"
./get_ncbi_pubmed $logdir $tempname $shared_data $config_file
echo "==========================================================="
echo 
echo "==========================================================="
echo "Getting homophila"
./get_homophila $logdir $tempname $shared_data
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting uniprot"
./get_uniprot $logdir $tempname $shared_data $config_file
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting Ensembl GeneId to PeptideId data"
./get_ensemblgeneID2peptideID $logdir $tempname $shared_data $config_file
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting InParanoid"
./get_inparanoid $logdir $tempname $shared_data $config_file
echo "==========================================================="
echo
#echo "==========================================================="
#echo "Getting Interpro xml file"
#./get_interproXML $logdir $tempname $shared_data
#echo "==========================================================="
echo
echo "==========================================================="
echo "Getting FlyBase version"
./get_flybase_version $logdir $tempname $shared_data
echo "==========================================================="

today=$(date +"%F")
logfile="/shared/data/download_logs/$today.txt"
mv $tempfile $logfile

file_with_usernames=${1:-./resources/mail_list}
for name in `cat ${file_with_usernames}`
do  
	mail -s "Outcome of data download run on $today" $name < $logfile
done

