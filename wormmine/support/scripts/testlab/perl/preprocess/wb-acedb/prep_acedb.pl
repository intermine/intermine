# Paulo Nuin August 2017
# based on multiple perl scripts for each class
# Wraps all <Txt> and <Text> elements in <![CDATA[ ]]>

use strict;


my %strnum = (
	1	=> "one",
	2	=> "two",
	3	=> "three",
	4	=> "four",
	5	=> "five",
	6	=> "six",
	7	=> "seven",
	8	=> "eight",
	9	=> "nine",
	0	=> "zero"
);

my ($infilename, $outfilename) = @ARGV;


&usage unless scalar @ARGV == 2;

my ($infile, $outfile);
open( $infile, $infilename) or die;
open( $outfile, '>'.$outfilename) or die;

my $strbuffer = '';
while(<$infile>){
	if(/^\n$/){
		&processpg;
		print $outfile "\n";
		$strbuffer = '';
	}
	$strbuffer .= $_;
}

sub processpg{
	return undef if $strbuffer eq "\n";
	
	$strbuffer =~ s/(<Text>|<Txt>)(.*?)(<[^ \d@-]+>)/$1<![CDATA[$2]]>$3/sg;
	$strbuffer =~ s/(<\/?)(\d+)(\w*>)/$1$strnum{$2}$3/sg;
	$strbuffer =~ s/&/&amp;/sg;
	print $outfile $strbuffer;
	
	return $strbuffer;
	
}

sub usage{
	print 
		"\nUsage: \n$0 <infile> <outfile>\n";
	die "\n";
}

