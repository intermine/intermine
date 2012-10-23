use strict;

use Test::More;
my $do_live_tests = $ENV{RELEASE_TESTING};

unless ($do_live_tests) {
    plan( skip_all => "Acceptance tests for release testing only" );
} else {
    plan( tests => 4 );
}

use Webservice::InterMine;

my $service = Webservice::InterMine->get_service('localhost/intermine-test');

my $path = $service->new_path('Department.employees.name');

is($path->get_possible_values_count, 132, "Gets the right no of emps");

$path = $service->new_path('Department.employees.name', 
    {"Department.employees" => 'Manager'});

is($path->get_possible_values_count, 30, "Gets the right number of managers");

$path = $service->new_path('Department.employees.name', 
    {'Department.employees' => 'CEO'} );

is($path->get_possible_values_count, 6, "Gets the right number of CEOs");

my $exp = [
    "Bwa'h Ha Ha",
    "Charles Miner",
    "EmployeeB1",
    "Jennifer Taylor-Clarke",
    "Juliette Lebrac",
    "Tatjana Berkel",
];

is_deeply([$path->get_possible_values], $exp, "Gets the correct CEO names");
