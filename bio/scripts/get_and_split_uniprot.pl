#!/usr/bin/perl
#by Andrew Vallejos

=head1 split_uniprot.pl

=begin text

#
# perl split_uniprot.pl <taxonomy_id>
#
# Purpose:
#	Download and Split a UniProt XML file into a Swiss-Prot and
#	a TeEMBL file.
#

=cut 

use LWP::UserAgent;
use strict;

my $taxon = shift(@ARGV);
&printHelp if $taxon eq '';

my $input = "${taxon}_uniprot_all.xml";
my $swiss = "${taxon}_uniprot_sprot.xml";
my $tremb = "${taxon}_uniprot_trembl.xml";

&getUniProtXML($taxon, $input);

&parseAndPrintXML($input, $swiss, $tremb);

exit(0);

###Subroutines###

sub printHelp
{
	print <<HELP;
#
# perl split_uniprot.pl <taxon_id>
#
# Purpose:
#	Download and Split a UniProt XML file into a Swiss-Prot and
#	a TeEMBL file.
#
HELP
	exit 0;
}

sub getUniProtXML
{
	my ($taxon, $input) = @_;
	my $url = "http://www.uniprot.org/uniprot/?query=taxonomy%3a${taxon}&force=yes&format=xml";

	my $ua = LWP::UserAgent->new;

	my $response;
	unless($response = $ua->get($url, ':content_file' => $input) and
		$response->is_success)
	{
		print "There was a problem fetching $url\n";
		print $response->status_line;
	}

	&validateFile($input);

}#end getUniProtXML

sub parseAndPrintXML
{

	open(IN, $input);
	open(SWISS, ">$swiss");
	open(TREMB, ">$tremb");

	my $entry = '';
	while(<IN>)
	{
		my $line = $_;
		if($line =~ /<entry/)
		{
			#start new entry
			my $entry = $line;
			while(<IN>)
			{
				$entry .= $_;
				last if $_ =~ m|</entry>|;
			}#end while
			
			#print entry to correct file
			if($entry =~ /dataset="Swiss-Prot"/i)
			{	print SWISS $entry;	}
			elsif($entry =~ /dataset="TrEMBL"/i)
			{	print TREMB $entry;	}
			$entry = '';
		}#end if($line =~/<entry/)
		else
		{
			print SWISS $line;
			print TREMB $line;
		}
	}#end while
	close IN;
	close SWISS;
	close TREMB;
	unlink($input);
}#end parseAndPrintXML

sub validateFile
{
	my $file = shift;
	my $size = -s $file;
	unless($size > 0)
	{
		print "There was a problem downloading the taxon requested.\n";
		print "Please check your taxon id and try again\n";
		unlink($file);
		exit(0);
	}
}