#!perl -T

use Test::More tests => 1;

BEGIN {
	use_ok( 'InterMine' );
}

diag( "Testing InterMine $InterMine::VERSION, Perl $], $^X" );
