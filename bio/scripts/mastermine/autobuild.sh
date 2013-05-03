#!/bin/bash
#
# default usage: autobuild.sh
#
# This script will automatically build mastermine on daily basis.

# TODO
# 	run script to download all data files
#	run project_build script
# 	copy database
# 	re-release webapp
#	run template comparison, acceptance tests, email devs

BASEDIR=/micklem/data

# Usage

# Download data
## homologene, orthodb, panther, treefam, zfin-identifiers, sgd-identifiers, ncbi, rgd-identifiers, worm-identifiers, go-annotation
perl bio/scripts/DataDownloader/bin/download_data -e intermine NCBIGene MGIIdentifiers RGDIdentifiers WormIdentifiers ZFINIdentifiers GOAnnotation HomoloGene OrthoDB Panther TreeFam    

## homologene
## mkdir `date +%d-%m-%Y`; cd `date +%d-%m-%Y`; wget ftp://ftp.ncbi.nih.gov/pub/HomoloGene/current/homologene.data; cd ..; rm current; ln -s `date +%d-%m-%Y` current

## orthodb
## mkdir `date +%d-%m-%Y`; cd `date +%d-%m-%Y`; wget ftp://cegg.unige.ch/OrthoDB6/OrthoDB6_ALL_METAZOA_tabtext.gzr; gunzip OrthoDB6_ALL_METAZOA_tabtext.gz; cd ..; rm current; ln -s `date +%d-%m-%Y` current

# Run project_build
../bio/scripts/project_build -b -v localhost ~/mastermine-dump

# Copy database
createdb -O fh293 -T mastermine mastermine-DATE

# Re-release webapp
cd mastermine/webapp
ant default remove-webapp release-webapp -Drelease=autobuild

# Template comparison - http://intermine.readthedocs.org/en/latest/database/data-integrity-checks/template-comparison/
# sudo easy_install intermine
cd intermine/scripts
python compare_templates_for_releases.py www.flymine.org/flymine beta.flymine.org/beta fh293@cam.ac.uk

# Acceptance tests - http://intermine.readthedocs.org/en/latest/database/data-integrity-checks/acceptance-tests/
cd mastermine/integrate
ant -v acceptance-tests

# Email devs