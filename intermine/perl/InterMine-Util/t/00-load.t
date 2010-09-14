#!perl -T

use Test::More tests => 1;

BEGIN {
    use_ok( 'InterMine::Util' ) || print "Bail out!
";
}

diag( "Testing InterMine::Util $InterMine::Util::VERSION, Perl $], $^X" );
