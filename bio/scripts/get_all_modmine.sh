#!/bin/sh
logdir="/shared/data/download_logs/"
if [ ! -d $logdir ]
then
	mkdir -p $logdir
fi
tempname="temp_log.txt"
tempfile=$logdir$tempname
modmine_data="/shared/data/modmine/sources"
shared_data="/shared/data"
config_file="./resources/get_scripts_modmine.config"
if [ -a $tempfile ]
then
	rm $tempfile;
fi

$errmsg="";

echo "==========================================================="
echo "Getting Fly Anatomy Ontology" 
./get_fly_anatomy_ontology $logdir $tempname $modmine_data
    if [ $? -ne 0 ]
    then
        echo "error getting Fly Anatomy Ontology" 2>&1        
#        exit 1
    fi
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting GO annotation"
./get_go-annotation $logdir $tempname $modmine_data $config_file
    if [ $? -ne 0 ]
    then
        echo "error gettingGO annotation" 2>&1
#        exit 1
    fi
echo
echo "==========================================================="
echo "Getting PubMed data"
./get_ncbi_pubmed $logdir $tempname $modmine_data $config_file
    if [ $? -ne 0 ]
    then
        "error getting PubMed data" 2>&1 >> tempfile
    fi

# echo "==========================================================="
# echo
# echo "==========================================================="
# echo "Getting Interpro xml file"
# ./get_interproXML $logdir $tempname $shared_data || (echo "error gettin InterPro XML file" 2>&1; exit 1)

# echo "==========================================================="
# echo
# echo "==========================================================="
# echo "Getting UniProt data"
# ./get_uniprot $logdir $tempname $shared_data "../../modmine/project.xml"
#     if [ $? -ne 0 ]
#     then
#         echo "error getting UniProt data" 2>&1
# #        exit 1
#     fi
echo "==========================================================="
echo
echo "==========================================================="
echo "Getting InParanoid data"
./get_inparanoid $logdir $tempname $modmine_data $config_file
    if [ $? -ne 0 ]
    then
        echo "error getting InParanoid data" 2>&1
#        exit 1
    fi
echo "==========================================================="
echo


today=$(date +"%F")
logfile="$logdir/$today.txt"
mv $tempfile $logfile

file_with_usernames=${1:-./resources/mail_list_modmine}
for name in `cat ${file_with_usernames}`
do  
	mail -s "Outcome of data download run on $today" $name < $logfile
done

