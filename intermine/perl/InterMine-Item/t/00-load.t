#!perl -T

use Test::More tests => 1;

BEGIN {
    use_ok( 'InterMine::Item' ) || print "Bail out!
";
}

diag( "Testing InterMine::Item $InterMine::Item::VERSION, Perl $], $^X" );
