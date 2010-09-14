#!perl -T

use Test::More tests => 1;

BEGIN {
    use_ok( 'InterMine::TypeLibrary' ) || print "Bail out!
";
}

diag( "Testing InterMine::TypeLibrary $InterMine::TypeLibrary::VERSION, Perl $], $^X" );
