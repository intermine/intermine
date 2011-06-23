#!perl -T

use Test::More tests => 1;

BEGIN {
    use_ok( 'InterMine::Model' ) || print "Bail out!
";
}

diag( "\nTesting InterMine::Model $InterMine::Model::VERSION, Perl $], $^X" );
