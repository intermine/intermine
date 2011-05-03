#!perl -T

use Test::More tests => 1;

BEGIN {
    use_ok( 'DataDownloader' ) || print "Bail out!
";
}

diag( "Testing DataDownloader $DataDownloader::VERSION, Perl $], $^X" );
