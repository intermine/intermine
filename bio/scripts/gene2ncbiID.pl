#!/usr/bin/perl -w
#creates a tab delimited file of ncbi gene numbers paired with either the identifier 
#or the organismDbId
use strict;

#define organism ids
my %ids=("4932",'S.cerevisiae',
	 "6239",'C.elegans',
	 "7227",'D.melanogaster',
	 "180454",'A.gambiae str PEST');
my (%IDdata,%DBdata);

#open /shared/data/ncbigene/gene_info
open(F,"<$ARGV[0]") or die "$!";
while(<F>){
	my @f = split/\t/;
	my $current_ID=$f[0];
	#only use the organisms in %ids
	if(exists $ids{$current_ID}){
  	  	my ($identifier,$dbID);
		my $ncbigeneID = $f[1];

		#use taxonID to get correct type of available data
		if($current_ID==180454 && $f[3] ne "-"){
	  		$identifier = $f[3];
			#check for duplicates
			if(exists $IDdata{$identifier}){
				print "$identifier already found\n"
			}else{
				$IDdata{$identifier}=$ncbigeneID;
				#print "$identifier\t$ncbigeneID\n";
			}
		}elsif($f[5] ne "-"){
			$dbID = $f[5];
	    	#remove excess characters
			if($dbID=~ /^SGD:/){
				$dbID=substr $dbID,4;
      		}elsif($dbID=~/^WormBase:/){
				$dbID=substr $dbID,9;
      		}elsif($dbID=~/^[Ff][Ll][Yy][Bb][Aa][Ss][Ee]:/){
				$dbID=substr $dbID,8;
      		}
			#check for duplicates
			if(exists $DBdata{$dbID}){
				print "$dbID already found\n"
			}else{
      		$DBdata{$dbID}=$ncbigeneID;
			#print "$dbID\t$ncbigeneID\n";
			}
    	}	
	}
}
close(F) or die "$!";
print "\nsaving...\n";

#write the results to a tab delimited file
my $ID;
open(RESULT, ">ncbigene2ID.txt") || die "$!";
foreach $ID (sort keys %IDdata){
  print RESULT "$ID\t$IDdata{$ID}\n";
}
close(RESULT);

my $DB;
open(RESULT, ">ncbigene2DB.txt") || die "$!";
foreach $DB (sort keys %DBdata){
  print RESULT "$DB\t$DBdata{$DB}\n";
}
close(RESULT);
