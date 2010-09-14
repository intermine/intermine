#!perl

use Test::More tests => 1;

BEGIN {
    use_ok( 'Webservice::InterMine' ) || print "Bail out!
";
}

diag( "Testing Webservice::Webservice::InterMine $Webservice::InterMine::VERSION, Perl $], $^X" );
