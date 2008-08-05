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
unlink($tempfile);

$errmsg="";

echo "==========================================================="
echo "Getting Fly Anatomy Ontology" 
./get_fly_anatomy_ontology $logdir $tempname $shared_data
    if [ $? -ne 0 ]
    then
        echo "error getting Fly Anatomy Ontology" 2>&1        
#        exit 1
    fi
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting FlyAtlas data"
./get_flyatlas $logdir $tempname $shared_data 
    if [ $? -ne 0 ]
    then
        echo "error getting FlyAtlas data" 2>&1
#        exit 1
    fi
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting GO annotation"
./get_go-annotation $logdir $tempname $shared_data $config_file 
    if [ $? -ne 0 ]
    then
        echo "error gettingGO annotation" 2>&1
#        exit 1
    fi
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting IntAct data"
./get_intact $logdir $tempname $shared_data $config_file
    if [ $? -ne 0 ]
    then
        echo "error gettingIntAct data" 2>&1
#        exit 1
    fi

echo "==========================================================="
echo
echo "==========================================================="
echo "Getting KEGG data"
./get_kegg $logdir $tempname $shared_data
    if [ $? -ne 0 ]
    then
        echo "error getting KEGG data" 2>&1
#        exit 1
    fi
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting WormBase identifiers"
./get_wormbase_identifiers $logdir $tempname $shared_data
    if [ $? -ne 0 ]
    then
        echo "error getting WormBase identifiers" 2>&1
#        exit 1
    fi
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting PubMed data"
./get_ncbi_pubmed $logdir $tempname $shared_data $config_file

    if [ $? -ne 0 ]
    then
        echo "error getting PubMed data" 2>&1
#        exit 1
    fi
echo "==========================================================="
echo 
echo "==========================================================="
echo "Getting Homophila data"
./get_homophila $logdir $tempname $shared_data
    if [ $? -ne 0 ]
    then
        echo "error getting Homophila data" 2>&1
#        exit 1
    fi

echo "==========================================================="
echo
echo "==========================================================="
echo "Getting UniProt data"
./get_uniprot $logdir $tempname $shared_data $config_file
    if [ $? -ne 0 ]
    then
        echo "error getting UniProt data" 2>&1
#        exit 1
    fi
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting Ensembl GeneId to PeptideId data"
./get_ensemblgeneID2peptideID $logdir $tempname $shared_data $config_file
    if [ $? -ne 0 ]
    then
        echo "error getting Ensembl GeneId to PeptideId data" 2>&1
#        exit 1
    fi
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting InParanoid data"
./get_inparanoid $logdir $tempname $shared_data $config_file 
    if [ $? -ne 0 ]
    then
        echo "error getting InParanoid data" 2>&1
#        exit 1
    fi
echo "==========================================================="
echo
#echo "==========================================================="
#echo "Getting Interpro xml file"
#./get_interproXML $logdir $tempname $shared_data || (echo "some error message" 2>&1; exit 1)
#echo "==========================================================="
#echo
echo "==========================================================="
echo "Getting FlyBase FASTA files"
./get_flybase $logdir $tempname $shared_data $config_file
    if [ $? -ne 0 ]
    then
        echo "error getting FlyBase FASTA files" 2>&1
#        exit 1
    fi
echo "==========================================================="

echo "==========================================================="
echo "Getting Ensembl Anopheles files"
./get_ensembl_anoph $logdir $tempname $shared_data 
    if [ $? -ne 0 ]
    then
        echo "error getting Ensembl Anopheles files" 2>&1
#        exit 1
    fi
echo "==========================================================="


today=$(date +"%F")
logfile="$logdir/$today.txt"
mv $tempfile $logfile

file_with_usernames=${1:-./resources/mail_list}
for name in `cat ${file_with_usernames}`
do  
	mail -s "Outcome of data download run on $today" $name < $logfile
done

