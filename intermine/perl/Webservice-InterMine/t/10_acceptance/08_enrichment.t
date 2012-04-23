use strict;

use Test::More;
use Webservice::InterMine;

my $do_live_tests = $ENV{RELEASE_TESTING};

unless ($do_live_tests) {
    plan( skip_all => "Acceptance tests for release testing only" );
} else {
    plan(tests => 3);
    my $service = get_service('localhost/intermine-test', "test-user-token");
    my $list = $service->list("Umlaut holders");
    is($list->size, 2);
    my $all = $list->enrichment(widget => "contractor_enrichment", maxp => 1)->get_all;
    is(~~@$all, 1);
    is($all->[0]{description}, "Ray");
    note explain $all;
}
