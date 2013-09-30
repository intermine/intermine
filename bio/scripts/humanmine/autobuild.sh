#!/bin/bash
#
# default usage: autobuild.sh
#
# This script will assist biologists to build humanMine.

BASEDIR=/micklem/data
SAN_HUMANMINE_DATA=/SAN_humanmine/data
SAN_HUMANMINE_DUMPS=/SAN_humanmine/dumps

echo "Autobuild script will help you to build humanMine in an interactive way."
echo ""
echo "Prerequisites:"
echo "* Run project build on theleviathan with correct configurations for Postgres and MySQL databases"
echo "* Email client is installed"
echo "* Check parsers are up-to-date with model changes and data format changes"
echo "* humanmine.properties in ./intermine directory"
echo "* Keep your git repository up-to-date"
echo "* Download and load Ensembl databases to MySQL"
echo "* Datasets need to download manually"
echo ""
echo "Note:"
echo "* Run this script from humanmine directory"
echo "* Dump directory"
echo "* Log directory"
echo ""

#================================ Functions ================================

update_datasets() {
    # Ref http://intermine.readthedocs.org/en/latest/database/download-scripts/?highlight=data%20download
    if [ -z "$1" ]
        then
            echo "Update all the datasets in intermine.yml"
            echo "Ref https://github.com/intermine/intermine/blob/dev/bio/scripts/DataDownloader/config/intermine.yml"
            echo ""
            perl ../bio/scripts/DataDownloader/bin/download_data -e intermine
    else
        echo "Update datasets: $@"
        echo ""
        perl ../bio/scripts/DataDownloader/bin/download_data -e intermine $@        
    fi
}

run_project_build() {
    date = `date +%d-%m-%Y`
    echo "Build humanMine database..."
    echo ""
    ../bio/scripts/project_build -l -v -Vbuild.theleviathan localhost $SAN_HUMANMINE_DUMPS/humanmine/theleviathan/humanmine-build-$date.final
    
    echo "Copy database humanmine-build to humanmine-$date"
    echo ""
    createdb -O fh293 -T humanmine-build humanmine-$date

    echo "Update database name in humanmine.properties.webapp.theleviathan"
    echo ""
    sed -i 's/^db.production.datasource.databaseName=.*/db.production.datasource.databaseName=humanmine-$date/' ~/.intermine/humanmine.properties.webapp.theleviathan
}

run_sources () {
    # Ref http://intermine.readthedocs.org/en/latest/database/database-building/build-script/#running-a-single-datasource
    if [ -z "$1" ]
        then
            echo "No sources supplied"
            echo ""
    else
        echo "Run sources: $1"
        echo ""
        (cd dbmodel/; ant clean build-db -Drelease=build.theleviathan) && (cd integrate; ant clean-all; ant -v -Dsource=$1 -Drelease=build.theleviathan)       
    fi  
}

run_a_postprocess () {
    # Ref http://intermine.readthedocs.org/en/latest/database/database-building/post-processing/?highlight=postprocess
    if [ -z "$1" ]
        then
            echo "No postprocess name supplied"
            echo ""
    else
        echo "Run postprocess: $1"
        echo ""
        (cd postprocess; ant -v -Daction=$1 -Drelease=webapp.theleviathan)       
    fi   
}

run_template_comparison () {
    # Ref http://intermine.readthedocs.org/en/latest/database/data-integrity-checks/template-comparison/
    # sudo easy_install intermine
    echo "Run template comparison..."
    echo ""
    (cd ../intermine/scripts; python compare_templates_for_releases.py www.flymine.org/humanmine www.metabolicmine.org/beta mike@intermine.org)
}

run_acceptance_tests () {
    # Ref http://intermine.readthedocs.org/en/latest/database/data-integrity-checks/acceptance-tests/?highlight=acceptance%20test
    echo "Run acceptance tests..."
    echo ""
    (cd integrate; ant acceptance-tests -Drelease=build.theleviathan)
    echo "The results will be in integrate/build/acceptance_test.html"

}

run_template_comparison_and_acceptance_tests () {
    run_template_comparison && run_acceptance_tests
}

release_webapp () {
    echo "Release webapp..."
    (cd webapp/; ant clean-all; ant default remove-webapp release-webapp -Drelease=webapp.theleviathan)
}

run_all_in_one () {
    echo "Update datasets -> Run project build -> Run template comparison -> Run acceptance tests -> Release webapp"
    update_datasets &&
    run_project_build &&
    run_template_comparison &&
    run_acceptance_tests &&
    release_webapp
}

#===========================================================================

# Ref http://stackoverflow.com/questions/226703/how-do-i-prompt-for-input-in-a-linux-shell-script
while true; do
    echo "Would you like to:" 
    echo "[1] Update all datasets by download script"
    echo "[2] Update any datasets"
    echo "[3] Run project build"
    echo "[4] Run datasources"
    echo "[5] Run a single postprocess"
    echo "[6] Run template comparison"
    echo "[7] Run acceptance tests"
    echo "[8] Run [6]&[7]"
    echo "[9] Release webapp"
    echo "[10] All-in-one"
    echo "[11] Exit"
    read -p "Please select one of the options: " num
    case $num in
        1  ) update_datasets; break;;
        2  ) echo "Please enter the dataset names separated by space:"; read DATASET_NAMES; update_datasets $DATASET_NAMES; break;;
        3  ) run_project_build; break;;
        4  ) echo "Please enter the sources separated by comma, e.g. omim,hpo:"; read SOURCE_NAMES; run_sources $SOURCE_NAMES; break;;
        5  ) echo "Please enter the postprocess:"; read POSTPROCESS; run_a_postprocess $POSTPROCESS; break;;
        6  ) run_template_comparison; break;;
        7  ) run_acceptance_tests; break;;
        8  ) run_template_comparison_and_acceptance_tests; break;;
        9  ) release_webapp; break;;
        10 ) run_all_in_one; break;;
        11 ) echo "Bye"; exit;;
        * ) echo "Please select.";;
    esac
done

# Email devs