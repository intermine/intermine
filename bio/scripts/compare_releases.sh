#!/bin/sh

# this script runs templates for two different releases and compares the results

mine='flymine'
release1='release-17.0'
release2='preview-18.0'


# release webapp
echo "changing to webapp directory"
cd ~/svn/dev/flymine/webapp
echo "building webapp"
ant default -Drelease=$release1 > ant_output.log

# run the performance_test script
echo "changing back to the script directory"
cd ~/svn/dev/bio/scripts
echo "running the performance test"
./performance_test flymine > $release1.tmp

# release webapp
echo "changing to webapp directory"
cd ~/svn/dev/flymine/webapp
echo "removing temporary directories"
ant clean-all > ant_output.log
echo "building webapp"
ant default -Drelease=$release2 > ant_output.log

# run the performance_test script
echo "changing back to the script directory"
cd ~/svn/dev/bio/scripts
echo "running the performance test"
./performance_test flymine > $release2.tmp

# compare the output
./compare_releases  $release1.tmp $release2.tmp > compare_releases.tmp

# let everyone know
mail -s "Outcome of data download run on $today" 'julie@flymine.org' < compare_releases.tmp
