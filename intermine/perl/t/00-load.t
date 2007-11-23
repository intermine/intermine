#!perl -T

use Test::More tests => 1;

BEGIN {
	use_ok( 'InterMine::ItemFactory' );
}

diag( "Testing InterMine $InterMine::ItemFactory::VERSION, Perl $], $^X" );
