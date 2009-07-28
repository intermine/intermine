#!/usr/bin/perl
#by Andrew Vallejos

=head1 split_uniprot.pl

=begin text

#
# perl split_uniprot.pl <uniprot_file>
#
# Purpose:
#	Split a UniProt XML file into a Swiss-Prot and
#	a TeEMBL file.
#

=cut 

use strict;

my $input = shift(@ARGV);
&printHelp if $input eq '';

my $swiss = "sprot.xml";
my $tremb = "trembl.xml";

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

sub printHelp
{
	print <<HELP;
#
# perl split_uniprot.pl <uniprot_file>
#
# Purpose:
#	Split a UniProt XML file into a Swiss-Prot and
#	a TeEMBL file.
#
HELP
	exit 0;
}