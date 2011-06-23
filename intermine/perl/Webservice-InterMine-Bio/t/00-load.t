#!perl -T

use Test::More tests => 1;

BEGIN {
    use_ok( 'Webservice::InterMine::Bio' ) || print "Bail out!
";
}

diag( "Testing Webservice::InterMine::Bio $Webservice::InterMine::Bio::VERSION, Perl $], $^X" );
