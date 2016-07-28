#!/usr/bin/perl -w

use FindBin qw/$Bin/;
use lib "$Bin/../perllib";
use strict;
use WormBase::Update::Intermine::WormBaseIdentifiers;
use Getopt::Long;

my ($release,$help);
GetOptions('release=s' => \$release,
	   'help=s'    => \$help);

if ($help || (!$release)) {
    die <<END;
    
Usage: $0 --release WSXXX

Fetch WormBase identifiers.

END
;
}

my $agent = WormBase::Update::Intermine::WormBaseIdentifiers->new({release => $release});
$agent->execute();
