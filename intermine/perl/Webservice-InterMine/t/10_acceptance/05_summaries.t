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
    $rs->summarize("age"),
    {
        'average' => '37.0243902439024390',
        'buckets' => 20,
        'histogram' => [
            1,1,1,2,4,11,11,5,6,4,8,9,11,4,4
        ],
        'max' => 49,
        'min' => 10,
        'stdev' => '7.3147502083982108'
    },
    "Can summarise all info"
) or diag(explain($rs->summarize('age')));

is('37.0243902439024390', $rs->summarize("age")->{average}, 
    "Can pull out a single value");

is_deeply(
    $rs->summarize("fullTime"),
    {
        '1' => 33,
        '0' => 49
    },
    "Can summarise all info, serialising booleans to 0/1"
);

is_deeply( 
    $rs->summarize("department.manager.name"),
    {
        'Keith Bishop' => 3,
        'EmployeeB1' => 1,
        'Meredith Palmer' => 4,
        'EmployeeA1' => 3,
        'Dr. Stefan Heinemann' => 6,
        'Glynn Williams' => 3,
        'Angela' => 4,
        'Burkhardt Wutke' => 5,
        'Bernd Stromberg' => 4,
        'Gilles Triquet' => 3,
        'Lonnis Collins' => 4,
        'Joel Liotard' => 2,
        "Didier Legu\x{e9}lec" => 5,
        'Slash Leader' => 3,
        'David Brent' => 3,
        "Frank M\x{f6}llers" => 3,
        "Sinan Tur\x{e7}ulu" => 5,
        'Separator Leader' => 3,
        'Timo Becker' => 4,
        'Jacques Plagnol Jacques' => 3,
        'Michael Scott' => 4,
        'XML Leader' => 2,
        'Neil Godwin' => 2,
        'Quote Leader' => 3
    },
    "Can summarise manager names",
);

is_deeply(
    [undef, 0, 2, 8, 9],
    [map {$_->{item}} $rs->results(size => 5, summaryPath => "end")],
    "Can get items in order",
);

