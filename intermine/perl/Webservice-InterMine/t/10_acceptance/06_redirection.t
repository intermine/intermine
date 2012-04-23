use strict;

use Test::More;
use Webservice::InterMine;

my $do_live_tests = $ENV{RELEASE_TESTING};

unless ($do_live_tests) {
    plan( skip_all => "Acceptance tests for release testing only" );
} else {
    # Should redirect to www.flymine.org
    my $service = get_service('flymine.org/query');
    my $query = $service->select("Organism.name");

    while (my $row = <$query>) {
        note @$row;
        is(@$row, 1);
    }
    my $c = $query->count;
    note $c;
    done_testing($c);
}

