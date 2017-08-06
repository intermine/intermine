#!/usr/bin/perl -w

use FindBin qw/$Bin/;
use lib "$Bin/../perllib";
use strict;
use WormBase::Update::Intermine::Interpro;
use Getopt::Long;

my ($release,$help);
GetOptions('release=s' => \$release,
	   'help=s'    => \$help);

if ($help || (!$release)) {
    die <<END;
    
Usage: $0 --release WSXXX

Fetch the latest interpro.xml file.

END
;
}

my $agent = WormBase::Update::Intermine::Interpro->new({release => $release});
$agent->execute();
