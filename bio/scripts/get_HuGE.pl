#!/usr/bin/perl

my $URLtoPostTo = "http://www.pathwaycommons.org/pc/webservice.do?version=2.0&q=BRCA2&format=xml&cmd=search";

my $BrowserName = "";

use strict;
use threads;
use threads::shared;
use LWP::UserAgent;
use HTTP::Request::Common;
use Data::Dumper;


my $outoutfile :shared; my $outputfile = "hugeOutput_20Jul09.tab";

my $Browser = new LWP::UserAgent;

if($BrowserName) { $Browser->agent($BrowserName); }
open(SOURCEFILE,"huge.html") || die "Error opening source file!\n";
my $prevone = '';
my %disease_hash=();
while(<SOURCEFILE>)
{
  	$_ =~ m{<a href="/HuGENavigator/huGEPedia\.do\?firstQuery=(.+)&geneID=(.+)&check=y&which=2&typeSubmit=GO&typeOption=gene&pubOrderType=pubD" onClick="return submitForm\(\)">};
	if($1 && ($1 ne $prevone))
	{	
		$disease_hash{$1}=$2;
		$prevone = $1;
  	}
}

$prevone = '';


#####################################
#Thread management variables
#####################################
my $active_threads :shared; $active_threads = 0;
my $max_threads :shared; $max_threads = 20;
my $total_processed :shared; $total_processed = 0;
#####################################
my $progress_counter :shared; $progress_counter = 0;
my @thread_array = ();
my $total_launched :shared; $total_launched = 0;
my $first_print :shared; $first_print = 1;
my @joinable_threads :shared = ();
my $size :shared; $size = scalar(keys %disease_hash);
my @geneNames = keys %disease_hash;
while($progress_counter < $size)
{
 	if(scalar(@thread_array)<$max_threads)
        {
                for(my $i=0; $i<$max_threads; $i++)
                {
                        my $geneName = shift(@geneNames);
                        #print "Initial Genename: $geneName\n";
                        $thread_array[$i] = threads->create('GetDisease', $geneName, $disease_hash{$geneName})
                }
        }
        else
        {
                my $pid = pop(@joinable_threads);
                if($pid ne "")
                {
                        #print "pid: $pid\n";
                        my $thr = threads->object($pid);
                        $thr->join();
                        my $geneName = shift(@geneNames);
                        if($geneName ne "")
                        {
                                #print "Genename: $genename\n";
                                $thr->create('GetDisease', $geneName, $disease_hash{$geneName});
                        }
                }
        }
}

#safety measure. make sure all threads completed before exiting
while ( my(@list)=threads->list()) {
#print "$#list\n";
grep { $_->join } @list;
};
exit;


sub GetDisease
{
	my $gene_name = @_[0];
	my $gene_id = @_[1];
	my $URL="http://hugenavigator.net/HuGENavigator/huGEPedia.do?firstQuery=$gene_name&geneID=$gene_id&check=y&which=2&typeSubmit=GO&typeOption=gene&pubOrderType=pubD";
	my $gene_page = $Browser->request(POST $URL);
	my @gene_page_lines = split(/\n/,$gene_page->content);
	foreach(@gene_page_lines)
	{
		$_ =~ m{<a href="/HuGENavigator/searchSummary\.do\?firstQuery=(.+)\+and\+(.+)&publitSearchType=now};
		if($1 && ($1 ne $prevone) && ($1 !~ m{genotype prevalence}) && ($1 !~ m{meta-analysis}) && ($2 !~ m{genotype prevalence}) && ($2 !~ m{meta-analysis}))
		{
			my $diseaseName = $1;
			my $pubURL = "http://hugenavigator.net/HuGENavigator/searchSummary.do?firstQuery=$1+and+$2&publitSearchType=now&whichContinue=firststart&check=y&dbType=publit&Mysubmit=go";
			my $pub_page = $Browser->request(POST $pubURL);
			my @pub_page_lines = split(/\n/,$pub_page->content);
			for(my $i = 0; $i < scalar(@pub_page_lines); $i++)
			{
				my $pubLine = $pub_page_lines[$i];
				if($pubLine =~ m{href="http://www\.ncbi\.nlm\.nih\.gov/entrez/query\.fcgi\?cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids=(.+)" target="new">(.+)</a>})
				{
					my $pubMedId = $1;
					my $paperTitle = $2;
				#as there are many lines per publication, check if this line is a new PMID... need a variable to track current PMID
				#if parsed a new PMID, means next publication
					my $j =($i)+5;
					my $breakCount = 1;
					my $pubString = "";
					my $authorList = "";
					while($pub_page_lines[$j] !~ m{</td>}) #until the end
					{
						if($breakCount < 3) #start
						{
							if(($pub_page_lines[$j]) !~ m{<br>} && (trim($pub_page_lines[$j]) ne ''))
							{
								$pubString = $pubString." ".trim($pub_page_lines[$j]);
							}
							if($pub_page_lines[$j] =~ m{<br>})
							{
								$breakCount++;
							}
						}
						else
						{
							if(trim($pub_page_lines[$j]) ne "")
							{
								$authorList = trim($pub_page_lines[$j]);
							}
						}
						$j++;
					}
					$pubString = trim($pubString);
					#print "$gene_name\t$pubMedId\n$diseaseName\n$paperTitle\n$pubString\n$authorList\n\n";
					my $outputString = "$gene_name\t$diseaseName\t$pubMedId\t$paperTitle\t$pubString\t$authorList\n";
					printresults($outputString);
				}
			}
			$prevone = $1;
		}
	}
        $active_threads--;
        $total_processed++;
        lock(@joinable_threads);
        push(@joinable_threads,threads->tid());
        $progress_counter++;
        my $percent_complete = int(($progress_counter/$size)*100);
        print "Now loading Gene: $gene_name\t\t$percent_complete% complete. $progress_counter out of $size.\n";
        return;
}

sub printresults
{
        my $inputstring = @_[0];
        lock($first_print);
        if($first_print > 0)
        {
                #print "First gene: $gene_symbol\n";
                open(OUTFILE, ">$outputfile");
                #print headers
		print OUTFILE "Gene Symbol\tCondition\tPubMed ID\tTitle\tPublication\tAuthors\n";
                $first_print = 0;
                #print "Firstprint $first_print\n";
                close(OUTFILE);
        }
        open(OUTFILE,">>$outputfile");
        print OUTFILE "$inputstring";
        close(OUTFILE);
        return;
}


#######################################################
# Subroutines
#######################################################
sub trim($)
{
        my $string = shift;
        $string =~ s/^\s+//;
        $string =~ s/\s+$//;
        return $string;
}

