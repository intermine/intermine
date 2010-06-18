#!/bin/sh

# this script runs templates for two different releases and compares the results

# $*

if test $# -ne 4
then
    echo "Please enter four arguments:  mine_name release1 release2 email_address"
    echo "eg. flymine release-22.0 preview-23.0 julie@flymine.org"
exit 1
else
    mine=$1
    release1=$2
    release2=$3
    mailto=$4
fi

INTERMINE_DIR="$HOME/svn/model_update"
MINE_DIR="$INTERMINE_DIR/$mine"


    run_performance_test() {
# release webapp                                                                                                                                                                                               
    echo "CHANGING TO WEBAPP DIRECTORY"
    cd $MINE_DIR/webapp
    echo "REMOVING TEMPORARY DIRECTORIES"
    ant clean-all > ant_output.log
    echo "BUILDING WEBAPP"
    ant default -v -Drelease=$release1 > $MINE_DIR/template_comparison_ant_output.log

# run the performance_test script                                                                                                                                                                              
    echo "CHANGING BACK TO THE SCRIPT DIRECTORY"
    cd $INTERMINE_DIR/bio/scripts
    echo "RUNNING THE PERFORMANCE TEST"
    ./performance_test $mine > $release1

# release webapp                                                                                                                                                                                               
    echo "changing to webapp directory"
    cd $MINE_DIR/webapp
    echo "removing temporary directories"
    ant clean-all >> ant_output.log
    echo "building webapp"
    ant default -v -Drelease=$release2 >> $MINE_DIR/template_comparison_ant_output.log
   
# run the performance_test script                                                                                                                                                                              
    echo "changing back to the script directory"
    cd $INTERMINE_DIR/bio/scripts
    echo "running the performance test"
    ./performance_test $mine > $release2

# compare the output                                                                                                                                                                                           
    ./compare_releases  $release1 $release2 > $MINE_DIR/compare_releases.tmp

# let everyone know                                                                                                                                                                                            
    mail -s "[$mine] Outcome of template comparison for $release1 and $release2" $mailto < $MINE_DIR/compare_releases.tmp
}

run_acceptance_tests() {
    echo "running acceptance tests"
    cd $INTERMINE_DIR/integrate
    ant acceptance-tests 

    attfile='~/svn/dev/' . $mine . '/integrate/build/acceptance_test.html'     
    subject="Outcome of acceptance tests run for $release2"
        
    mutt -s "$subject" -a $attfile $mailto < /dev/null
}
run_performance_test
#run_acceptance_tests

echo "done"




