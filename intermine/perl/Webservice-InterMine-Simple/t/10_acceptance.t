use strict;
use warnings;

use Test::More;

use Webservice::InterMine::Simple 0.9800;

if (not $ENV{RELEASE_TESTING}) {
    plan(skip_all => "Acceptance tests for release testing");
} else {
    plan tests => 25;

    my $service = get_service("http://www.flymine.org/query/service");

    my $query = $service->new_from_xml(source_file => "t/data/org_query.xml");

    my @rows = $query->results_table;

    is(23, scalar(@rows));
    is(23, $query->get_count);

    is $rows[0][0], "Anopheles gambiae";

    my $query2 = $service->new_query;

    $query2->add_view("Organism.shortName", "Organism.taxonId");
    $query2->add_constraint({path => "Organism.genus", op => "=", value => "Rattus"});

    note $query2->as_xml;

    @rows = $query2->results_table;

    is(1, scalar(@rows));

    is $rows[0][0], "R. norvegicus";

    my $template = $service->template("Gene_Pathway");

    @rows = $template->results_table(
        constraint1 => "Gene", op1 => "LOOKUP", value1 => "bsk"
    );

    for my $row (@rows) {
        is("bsk", $row->[1]);
    }

    is $template->get_count(constraint1 => "Gene", op1 => "LOOKUP", value1 => "bsk"), 19;

}
