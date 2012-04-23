use strict;

use Test::More;
use Webservice::InterMine;

my $do_live_tests = $ENV{RELEASE_TESTING};

unless ($do_live_tests) {
    plan( skip_all => "Acceptance tests for release testing only" );
} else {
    plan( tests => 7);
    my $service = get_service('localhost/intermine-test');
    my $obj = $service->resultset("Employee")->select("*")->where(name => "David Brent")->first(as => 'objects');

    is($obj->name, "David Brent");
    is($obj->department->name, "Sales");
    is_deeply(
        [map {$_->name} $obj->department->employees], 
        ["David Brent", "Ricky", "Rachel","Tim Canterbury", "Gareth Keenan", "Malcolm",],
        "Can fetch all members of a collection"
    );
    is($obj->department->employees_count, 6);

    # For all members of result set (bug reported by JD-Wong: 03-02-12).
    my @emps = $service->resultset("Employee")->where("department.manager.name" => "David Brent")->results(as => "objects");
    my @addresses = map {$_->address->address} @emps;
    my $expected = [
        '13 Confusion Row',
        '31 Worker Av',
        '18 Worker Av',
        '83 Worker Av',
        '23 Worker Av',
        '22 Worker Av'
    ];
    is_deeply(
        \@addresses,
        $expected,
        "Can get all x's of a set of y's"
    ) or diag(explain \@addresses);

    # Laxy attributes
    my $david = $service->resultset("Employee")->select("department.company.name")->where(name => "David Brent")->first(as => 'objects');
    my $company = $david->department->company;

    is($company->name, "Wernham-Hogg", "Got what was selected");
    is($company->vatNumber, 392018, "And what was not");


}

