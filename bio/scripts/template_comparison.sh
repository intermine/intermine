#!/bin/sh

# this script runs templates for two different releases and compares the results

mine='flymine'
release1='release-19.0'
release2='preview-20.0'
#mailto='julie@flymine.org'
mailto='all@flymine.org'

run_performance_test() {
# release webapp                                                                                                                                                                                               
    echo "changing to webapp directory"
    cd ~/svn/dev/flymine/webapp
    echo "removing temporary directories"
    ant clean-all > ant_output.log
    echo "building webapp"
    ant default -Drelease=$release1 > ant_output.log
    
# run the performance_test script                                                                                                                                                                              
    echo "changing back to the script directory"
    cd ~/svn/dev/bio/scripts
    echo "running the performance test"
    ./performance_test flymine > $release1

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
    ./performance_test flymine > $release2

# compare the output                                                                                                                                                                                           
    ./compare_releases  $release1 $release2 > compare_releases.tmp

# let everyone know                                                                                                                                                                                            
    mail -s "Outcome of template comparison comparing $release1 and $release2" $mailto < compare_releases.tmp    
}

run_acceptance_tests() {
    echo "running acceptance tests"
    cd ~/svn/dev/flymine/integrate
    ant acceptance-tests 

    attfile='/home/julie/svn/dev/flymine/integrate/build/acceptance_test.html'     
    subject="Outcome of acceptance tests run for $release2"
        
    mutt -s "$subject" -a $attfile $mailto < /dev/null
}
run_performance_test
run_acceptance_tests
echo "done"



