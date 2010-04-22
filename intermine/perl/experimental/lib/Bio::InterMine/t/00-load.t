#!perl -T

use Test::More tests => 2;

BEGIN {
    use_ok( 'Bio::InterMine::Query' ) || print "Bail out!
";
    use_ok( 'Bio::InterMine::Model' ) || print "Bail out!
";
}

diag( "Testing Bio::InterMine::Query $Bio::InterMine::Query::VERSION, Perl $], $^X" );
