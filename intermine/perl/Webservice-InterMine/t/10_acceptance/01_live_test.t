package main;

use strict;
use warnings;

use Carp qw/cluck/;

use Test::More;
use Test::Exception;

use MooseX::Types::Moose qw(Bool);

my $do_live_tests = $ENV{RELEASE_TESTING};

unless ($do_live_tests) {
    plan( skip_all => "Acceptance tests for release testing only" );
} else {
    plan( tests => 191 );
}

my $module = 'Webservice::InterMine';

my $url = 'http://localhost:8080/intermine-test/service';
my @view = ('Employee.name', 'Employee.age', 'Employee.fullTime',
    'Employee.address.address', 'Employee.department.name',
    'Employee.department.company.name',
    'Employee.department.manager.name',
);

use_ok($module, ($url));

isa_ok($module->get_service, 'Webservice::InterMine::Service', "The service it makes");

throws_ok(
    sub {$module->get_service("not.a.good.url")},
    qr/Could not resolve/,
    "Throws an error at bad urls",
);

throws_ok(
    sub {$module->get_service("not.a.good.url/with/path")},
    qr/Can't connect/,
    "Throws an error at bad urls",
);

throws_ok(
    sub {$module->get_service("http://localhost:8080/intermine-test/foo/")},
    qr/version.*please check the url/,
    "Throws an error at bad urls",
);

ok($module->get_service->version >= 6, "Service version is correct");
isa_ok($module->get_service->model, 'InterMine::Model', "The model the service makes");
my $q;
lives_ok(sub {$q = $module->new_query}, "Makes a new query ok");
isa_ok($q, 'Webservice::InterMine::Query', "The query");

lives_ok(sub {$q->add_view(@view)}, "Adds a view to the query ok");
is_deeply($q->view, \@view, "Sets view correctly");

lives_ok(
    sub {
        $q->add_constraint(
            path => 'Employee.age',
            op   => '>',
            value => 16,
        );
    },
    "Adds a binary constraint to the query ok",
);

lives_ok(
    sub {
        $q->add_constraint(
            path => 'Employee.department',
            op   => 'IS NOT NULL',
        );
    },
    "Adds a unary constraint to the query ok",
);


lives_ok(
    sub {
        $q->add_constraint(
            path => 'Employee.department',
            op   => 'LOOKUP',
            value => 'Catering'
        );
    },
    "Adds a lookup constraint to the query ok",
);


lives_ok(
    sub {
        $q->add_constraint(
            path => 'Employee.name',
            op   => 'ONE OF',
            values => [qw/Susan John Miguel/],
        );
    },
    "Adds a multi constraint to the query ok",
);

lives_ok(
    sub {
        $q->add_constraint(
            path => 'Employee',
            type => 'CEO',
        );
    },
    "Adds a subclass constraint to the query ok",
);

is($q->all_constraints, 5, "All constraints added fine");
is($q->coded_constraints, 4, "And 4 of them have codes");
is($q->sub_class_constraints, 1, "And one of them is a sub-class constraint");

lives_ok(
    sub {$q->set_sort_order('Employee.age', 'desc')},
    "Sets sort order",
);

is($q->sort_order, "Employee.age desc", "And it is correct");

my $res;

$q = $module->new_query;
$q->add_view(@view);
$q->add_constraint(
    path => "Employee.department.company",
    op => "LOOKUP",
    value => "CompanyA"
);

note("Querying for results");

lives_ok(
    sub {$res = $q->results},
    "Queries for results",
) or diag($q->url);

is(ref $res, 'ARRAY', "And it is an arrayref");

is(ref $res->[0], 'Webservice::InterMine::ResultRow', "An array of result-rows in fact");

is($res->[1][1], "20", "With the right fields - Int") or diag(explain $res);;
is($res->[1][3], "Employee Street, AVille", "With the right fields - Str") or diag(explain $res);;

my $res_slice = [
  'EmployeeA2',
  '20',
  'true',
  'Employee Street, AVille',
  'DepartmentA1',
  'CompanyA',
  'EmployeeA1'
];

is_deeply($q->results(size => 1, start => 1)->[0]->to_aref, $res_slice, "Can handle start and size");

$q->add_constraint(
    path  => 'Employee.age',
    op    => '<',
    value => 35,
);

lives_ok(
    sub {$res = $q->results(as => 'hashrefs')},
    "Queries for results as hashes",
);

is(@$res, 3, "Gets the right number of records");
is($res->[1]->{'Employee.age'}, 20, "with the right fields - Int");
is($res->[1]->{'Employee.address.address'}, "Employee Street, AVille", "with the right fields - Str");

lives_ok(
    sub {$res = $q->results(as => 'arrayrefs')},
    "Queries for results as arrayrefs",
);

is(@$res, 3, "Gets the right number of records");
is($res->[1][1], 20, "with the right fields - Int");
is($res->[1][3], "Employee Street, AVille", "with the right fields - Str");


lives_ok(
    sub {$res = $q->results(as => 'jsonobjects', json => 'perl')},
    "Queries for results as a perl data structure",
);

is(@$res, 3, "Gets the right number of records");
is($res->[1]{age}, 20, "with the right fields - Int");
is($res->[1]{address}{address}, "Employee Street, AVille", "with the right fields - Str");
ok($res->[1]{fullTime}, "with the right fields - Bool");

is(3, $q->count, "Can get a count");

lives_ok(
    sub {$res = $q->results(as => 'jsonobjects', json => 'raw')},
    "Queries for results as a raw text",
);
my $expected = [
    qr|\Q{"address":{"address":"Employee Street, AVille","objectId":\E\d+\Q,"class":"Address"},"objectId":\E\d+\Q,"department":{"manager":{"objectId":\E\d+\Q,"name":"EmployeeA1","class":"Manager"},"objectId":\E\d+\Q,"name":"DepartmentA1","company":{"objectId":\E\d+\Q,"name":"CompanyA","class":"Company"},"class":"Department"},"age":10,"name":"EmployeeA1","class":"Manager","fullTime":true}|,
    qr|\Q{"address":{"address":"Employee Street, AVille","objectId":\E\d+\Q,"class":"Address"},"objectId":\E\d+\Q,"department":{"manager":{"objectId":\E\d+\Q,"name":"EmployeeA1","class":"Manager"},"objectId":\E\d+\Q,"name":"DepartmentA1","company":{"objectId":\E\d+\Q,"name":"CompanyA","class":"Company"},"class":"Department"},"age":20,"name":"EmployeeA2","class":"Employee","fullTime":true}|,
    qr|\Q{"address":{"address":"Employee Street, AVille","objectId":\E\d+\Q,"class":"Address"},"objectId":\E\d+\Q,"department":{"manager":{"objectId":\E\d+\Q,"name":"EmployeeA1","class":"Manager"},"objectId":\E\d+\Q,"name":"DepartmentA1","company":{"objectId":\E\d+\Q,"name":"CompanyA","class":"Company"},"class":"Department"},"age":30,"name":"EmployeeA3","class":"Employee","fullTime":false}|,
];

is (scalar(@$res), scalar(@$expected), "It has the right number of rows");
for (0 .. $#{$expected}) {
    like($res->[$_], $expected->[$_], "And row $_ looks good");
}

lives_ok(
    sub {$res = $q->results(as => 'jsonobjects', json => 'inflate')},
    "Queries for results as inflated objects",
);

is(@$res, 3, "Gets the right number of records");
is($res->[1]->age, 20, "with the right fields - Int");
is($res->[1]->address->address, "Employee Street, AVille", "with the right fields - Str");
ok($res->[1]->fullTime, "with the right fields - Bool");

lives_ok(
    sub {$res = $q->results(as => 'jsonobjects', json => 'instantiate')},
    "Queries for results as inflated objects",
);

is(@$res, 3, "Gets the right number of records");
is($res->[1]->getAge, 20, "with the right fields - Int");
is($res->[1]->getAddress->getAddress, "Employee Street, AVille", "with the right fields - Str");

PRINTING: {
    my $buffer = '';
    open(my $fh, '>', \$buffer) or die "Horribly, $!";
    $q->print_results(to => $fh, columnheaders => 1);
    close $fh or die "$!";
    my $expected = qq|"Employee > Name"\t"Employee > Years Alive"\t"Employee > Works Full Time?"\t"Employee > Lives At"\t"Employee > Works In"\t"Employee > Works For"\t"Employee > Works Under"
"EmployeeA1"\t"10"\t"true"\t"Employee Street, AVille"\t"DepartmentA1"\t"CompanyA"\t"EmployeeA1"
"EmployeeA2"\t"20"\t"true"\t"Employee Street, AVille"\t"DepartmentA1"\t"CompanyA"\t"EmployeeA1"
"EmployeeA3"\t"30"\t"false"\t"Employee Street, AVille"\t"DepartmentA1"\t"CompanyA"\t"EmployeeA1"
|;
    for ($buffer, $expected) {
        s/\t/[TAB]/g;
        s/ /./g;
        s/\r/[CR]/g;
        s/\n/¬\n/g;
    }
    is $buffer, $expected, "Can print a query";
}

SHOWING: {
    my $buffer = '';
    open(my $fh, '>', \$buffer) or die "Horribly, $!";
    $q->show($fh);
    close $fh or die "$!";
    my $expected = q!VIEW:.[Employee.name,.Employee.age,.Employee.fullTime,.Employee.address.address,.Employee.department.name,.Employee.department.company.name,.Employee.department.manager.name],.CONSTRAINTS:.[<Employee.department.company.LOOKUP."CompanyA".IN."NULL">,<Employee.age.<."35">,],.LOGIC:.A.and.B,.SORT_ORDER:.Employee.name.asc
--------------+--------------+-------------------+--------------------------+--------------------------+----------------------------------+---------------------------------
Employee.name.|.Employee.age.|.Employee.fullTime.|.Employee.address.address.|.Employee.department.name.|.Employee.department.company.name.|.Employee.department.manager.name
--------------+--------------+-------------------+--------------------------+--------------------------+----------------------------------+---------------------------------
EmployeeA1....|.10...........|.true..............|.Employee.Street,.AVille..|.DepartmentA1.............|.CompanyA.........................|.EmployeeA1......................
EmployeeA2....|.20...........|.true..............|.Employee.Street,.AVille..|.DepartmentA1.............|.CompanyA.........................|.EmployeeA1......................
EmployeeA3....|.30...........|.false.............|.Employee.Street,.AVille..|.DepartmentA1.............|.CompanyA.........................|.EmployeeA1......................
!;
    for ($buffer, $expected) {
        s/\t/[TAB]/g;
        s/ /./g;
        s/\r/[CR]/g;
    }
    is $buffer, $expected, "Can show a query";
}

my $t;
lives_ok(
    sub {$t = $module->template('employeesFromCompanyAndDepartment');},
    "Gets a template ok",
);

isa_ok($t, 'Webservice::InterMine::Query::Template', "The template");

is($t->editable_constraints, 2, "And it has 2 editable constraints");

lives_ok(
    sub {$res = $t->results_with(valueA => "CompanyB");},
    "Runs results with ok",
) or diag($t->url);

my $exp_res = [
    ['EmployeeB1','40'],
    ['EmployeeB2','50'],
    ['EmployeeB3', '60']
];

for my $row (0, 1, 2) {
    for my $col (0, 1) {
        is($res->[$row][$col], $exp_res->[$row][$col]);
    }
}

$exp_res = [
    ['EmployeeA1','10'],
    ['EmployeeA2','20'],
    ['EmployeeA3','30']
];

$res = $t->results;

for my $row (0, 1, 2) {
    for my $col (0, 1) {
        is($res->[$col][$row], $exp_res->[$col][$row]);
    }
}

$exp_res = ['EmployeeA2',20];

is_deeply($t->results(size => 1, start => 1)->[0]->to_aref, $exp_res, "And it handles start and size");

$exp_res = [
    {
        'age' => 10,
        'class' => 'Manager',
        'name' => 'EmployeeA1',
    },
    {
        'age' => 20,
        'class' => 'Employee',
        'name' => 'EmployeeA2',
    },
    {
        'age' => 30,
        'class' => 'Employee',
        'name' => 'EmployeeA3',
    }
];
$res = $t->results(as => "jsonobjects");
for my $r (@$res) {
    delete $r->{objectId}; # Horrifically ugly, but we cannot rely on these to be consistent.
}
is_deeply($res, $exp_res, "And for complex formats") or diag(explain $res);
$res = $t->results(as => "jsonobjects", json => 'inflate');
is($res->[0]->name, "EmployeeA1", "Can access inflated columns ok");

subtest "and for json rows" => sub {
    $res = $t->results(as => "jsonrows");
    is($res->[0][0]{value}, "EmployeeA1") or diag(explain $res->[0]);
    is($res->[0][1]{value}, 10) or diag(explain $res->[0]);
    is($res->[2][0]{value}, "EmployeeA3") or diag(explain $res->[2]);
    $res = $t->results(as => "jsonrows", json => "inflate");
    is($res->[0][0]->value, "EmployeeA1") or diag(explain $res->[0]);
    is($res->[0][1]->value, 10) or diag(explain $res->[0]);
    is($res->[2][0]->value, "EmployeeA3") or diag(explain $res->[2]);
};

SHOWING_TEMPLATES: {
    my $buffer = '';
    open(my $fh, '>', \$buffer) or die "Horribly, $!";
    $t->show_with(valueA => 'companyB', to => $fh);
    close $fh or die "$!";
    my $expected = q!employeesFromCompanyAndDepartment.-.View.all.the.employees.that.work.within.a.certain.department.of.the.specified.company
--------------+-------------
Employee.name.|.Employee.age
--------------+-------------
EmployeeB1....|.40..........
EmployeeB2....|.50..........
EmployeeB3....|.60..........
!;
    $buffer =~ s/ /./g;
    $expected =~ s/ /./g;
    
    is $buffer, $expected, "Can show a template";
}

PRINTING_TEMPLATES: {
    my $buffer = '';
    open(my $fh, '>', \$buffer) or die "Horribly, $!";
    $t->print_results_with(valueA => 'companyB', to => $fh, columnheaders => 1);
    close $fh or die "$!";
    my $expected = qq|"Employee.>.Name"\t"Employee.>.Years Alive"
"EmployeeB1"\t"40"
"EmployeeB2"\t"50"
"EmployeeB3"\t"60"
|;
    $buffer =~ s/ /./g;
    $expected =~ s/ /./g;
    
    is $buffer, $expected, "Can print a template";
}

my $loaded;
lives_ok {$loaded = $module->load_query(source_file => "t/data/loadable_query.xml")} 
    "Can load a query";

$exp_res = [ ['EmployeeA1','DepartmentA1'] ];

$res = $loaded->results;
is($res->[0][0], $exp_res->[0][0], "Can get results for queries loaded from xml");
is($res->[0][1], $exp_res->[0][1], "Can get results for queries loaded from xml");

AUTHENTICATION: {
    require Webservice::InterMine::Service;
    my $authenticated_service;
    my @password_credentials = ("intermine-test-user", "intermine-test-user-password");
    my $token = "test-user-token";

    my $token_service = Webservice::InterMine::Service->new($url, $token);

    is($token_service->token, $token, "Interprets arguments correctly as token");

    my $template2 = $token_service->template("private-template-1");

    is($template2->get_count, 53, "Can read a private template using a token service");
    
    my $foolish_auth_method = sub {
        $authenticated_service = Webservice::InterMine::Service->new($url, @password_credentials);
    };

    SKIP: {
        unless (eval "require Test::Warn;") {
            eval {
                no warnings; 
                $foolish_auth_method->();
            };
            skip "Test Warn not installed", 1;
        } else {
            Test::Warn::warning_like($foolish_auth_method, qr/API token/, 
                "Warns people who are not careful with their passwords"
            );
        }
    }

    my $template = $authenticated_service->template("private-template-1");

    is($template->get_count, 53, "Can read a private template using username/password credentials");
}


PARSING_EMPTY_RESULTS: {
    my $q = $module->new_query(class => 'Manager');
    $q->add_views('*');
    $q->add_constraint('name', '=', 'Santa Claus');
    my $res;
    lives_ok {$res = $q->results} "It's ok to ask about Santa Claus";
    is_deeply($res, [], "But there is no Santa Claus");
}

DBIX_SUGAR: {
    my @results = $module->get_service
                         ->resultset('Manager')
                         ->search({'department.name' => 'Sales'});

    is(@results, 3, "Search returns result");
    is_deeply(
        [ 'David Brent', 'Michael Scott', 'Gilles Triquet',], 
        [map {$_->getName} @results], 
        "And they have the expected content - reified objects"
    ) or diag explain(\@results);

}

TEST_IMPORTED_FNS: {
    my @results = resultset("Manager")->search({"department.name" => 'Sales'});
    is(@results, 3, "Can get results with search");
    is_deeply(
        [ 'David Brent', 'Michael Scott', 'Gilles Triquet',], 
        [map {$_->getName} @results], 
        "And they have the expected content - reified objects"
    ) or diag explain(\@results);
    
    my $res = get_template('employeesFromCompanyAndDepartment')->results_with(valueA => "CompanyB");
    my $exp_res = [
        ['EmployeeB1','40'],
        ['EmployeeB2','50'],
        ['EmployeeB3', '60']
    ];
    for my $row (0, 1, 2) {
        for my $col (0, 1) {
            is($res->[$row][$col], $exp_res->[$row][$col], "Results are rows, as expected");
        }
    }

    is ($module->get_service->version, get_service()->version, "Testing get_service");

}

TEST_LIST_STATUS: {
    my @lists = get_service("www.flymine.org/query")->get_lists();
    ok($lists[0]->has_status, "Status is provided");
    my %possible_statuses = (CURRENT => 1, TO_UPGRADE => 1, NOT_CURRENT => 1);
    ok($possible_statuses{$lists[0]->status}, "And list is one of the possible statuses");
}

TEST_DEFAULT_FORMATS: {
    my $query = resultset("Manager")->select("name", "department.name");
    my $rr = "Webservice::InterMine::ResultRow";
    while (my $row = <$query>) {
        ok($row->isa($rr), "isa result-row");
    }

    my $ro = "Webservice::InterMine::ResultObject";
    my $it = $query->iterator(as => 'ro');
    while (my $row = <$it>) {
        ok($row->isa($ro), "isa result-object");
    }

    my $class = "Manager";
    $it = $query->iterator(as => 'objects');
    while (my $row = <$it>) {
        ok($row->isa($class), "isa Manager");
    }
}

