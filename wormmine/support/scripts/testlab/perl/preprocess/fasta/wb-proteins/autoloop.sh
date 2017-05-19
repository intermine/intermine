for i in `ls /home/jdmswong/intermine-dev/datadir/fasta/c_elegans/proteins/raw/*.fa`; do echo ; echo "perl prep-wb-proteins.pl $i /home/jdmswong/intermine-dev/datadir/fasta/c_elegans/proteins/prepped/prepped-`basename $i` "; done

# problem: ls is aliased to ls -S, shows filesizes for each row.  This disrupts this loop, thinks filesize is a file name too.  Fix
