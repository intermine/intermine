#!/usr/bin/perl -w

use FindBin qw/$Bin/;
use lib "$Bin/../perllib";
use strict;
use WormBase::Update::Intermine::PhenotypeOntology;
use Getopt::Long;

my ($release,$help);
GetOptions('release=s' => \$release,
	   'help=s'    => \$help);

if ($help || (!$release)) {
    die <<END;
    
Usage: $0 --release WSXXX

Fetch the phenotype obo and association files.

END
;
}

my $agent = WormBase::Update::Intermine::PhenotypeOntology->new({release => $release});
$agent->execute();
