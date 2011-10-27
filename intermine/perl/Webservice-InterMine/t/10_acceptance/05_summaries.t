use strict;

use Test::More;

my $do_live_tests = $ENV{RELEASE_TESTING};

unless ($do_live_tests) {
    plan( skip_all => "Acceptance tests for release testing only" );
} else {
    plan( tests => 5 );
}

use Webservice::InterMine;

my $service = Webservice::InterMine->get_service('localhost/intermine-test');

my $rs = $service->resultset("Employee")
                 ->add_to_select("department.manager.name")
                 ->where("age" => {lt => 50});

is_deeply(
    {
        'stdev' => '7.9045870350591347',
        'average' => '36.3561643835616438',
        'min' => 10,
        'max' => 49
    },
    $rs->summarize("age"),
    "Can summarise all info"
);

is('36.3561643835616438', $rs->summarize("age")->{average}, 
    "Can pull out a single value");

is_deeply(
    {
        '1' => 32,
        '0' => 41
    },
    $rs->summarize("fullTime"),
    "Can summarise all info, serialising booleans to 0/1"
);

is_deeply( 
    {
        'Keith Bishop' => 3,
        'EmployeeB1' => 1,
        'Meredith Palmer' => 4,
        'EmployeeA1' => 3,
        'Dr. Stefan Heinemann' => 4,
        'Glynn Williams' => 1,
        'Angela' => 2,
        'Burkhardt Wutke' => 3,
        'Bernd Stromberg' => 6,
        'Gilles Triquet' => 3,
        'Lonnis Collins' => 4,
        "Didier Legu\x{e9}lec" => 3,
        'Slash Leader' => 1,
        'David Brent' => 3,
        "Frank M\x{f6}llers" => 4,
        "Sinan Tur\x{e7}ulu" => 5,
        'Separator Leader' => 3,
        'Timo Becker' => 2,
        'Jacques Plagnol Jacques' => 5,
        'Michael Scott' => 5,
        'XML Leader' => 3,
        'Neil Godwin' => 2,
        'Quote Leader' => 3
    },
    $rs->summarize("department.manager.name"),
    "Can summarise manager names",
);

is_deeply(
    [undef, 0, 2, 3, 4],
    [map {$_->{item}} $rs->results(size => 5, summaryPath => "end")],
    "Can get items in order",
);

