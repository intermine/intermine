package main;

use strict;
use warnings;

use Carp qw/cluck/;

use Test::More;
use Test::Exception;

use MooseX::Types::Moose qw(Bool);

$SIG{__WARN__} = \&Carp::cluck;

my $do_live_tests = $ENV{RELEASE_TESTING};

unless ($do_live_tests) {
    plan( skip_all => "Acceptance tests for release testing only" );
} else {
    plan( tests => 59 );
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
    qr/Can't connect/,
    "Throws an error at bad urls",
);

throws_ok(
    sub {$module->get_service("http://localhost:8080/intermine-test/foo/")},
    qr/version.*please check the url/,
    "Throws an error at bad urls",
);

is($module->get_service->version, 3, "Service version is correct");
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

#diag($q2->url, "=>\n" , $q2->results(as => 'string'));

lives_ok(
    sub {$res = $q->results},
    "Queries for results",
) or diag($q->url);

is(ref $res, 'ARRAY', "And it is an arrayref");

is(ref $res->[0], 'ARRAY', "An array of arrays in fact");

is($res->[1][1], "20", "With the right fields - Int") or diag(explain $res);;
is($res->[1][3], "Employee Street, AVille", "With the right fields - Str") or diag(explain $res);;

my $res_slice = [[
  'EmployeeA2',
  '20',
  'true',
  'Employee Street, AVille',
  'DepartmentA1',
  'CompanyA',
  'EmployeeA1'
]];

is_deeply($q->results(size => 1, start => 1), $res_slice, "Can handle start and size");

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
is($res->[1]->{'Employee.age'}, "20", "with the right fields - Int");
is($res->[1]->{'Employee.address.address'}, "Employee Street, AVille", "with the right fields - Str");

lives_ok(
    sub {$res = $q->results(as => 'jsonobjects', json => 'perl')},
    "Queries for results as a perl data structure",
);

is(@$res, 3, "Gets the right number of records");
is($res->[1]{age}, 20, "with the right fields - Int");
is($res->[1]{address}{address}, "Employee Street, AVille", "with the right fields - Str");
ok($res->[1]{fullTime}, "with the right fields - Bool");

lives_ok(
    sub {$res = $q->results(as => 'jsonobjects', json => 'raw')},
    "Queries for results as a raw text",
);

my $expected_re = qr/\Q{'rootClass':'Employee','modelName':'testmodel','start':0,'views':["Employee.age","Employee.fullTime","Employee.name","Employee.address.address","Employee.department.name","Employee.department.company.name","Employee.department.manager.name"],'results':[{"address":{"address":"Employee Street, AVille","objectId":3000019,"class":"Address"},"objectId":3000015,"department":{"manager":{"objectId":3000015,"name":"EmployeeA1","class":"Manager"},"objectId":3000014,"name":"DepartmentA1","company":{"objectId":3000000,"name":"CompanyA","class":"Company"},"class":"Department"},"age":10,"name":"EmployeeA1","class":"Manager","fullTime":true},{"address":{"address":"Employee Street, AVille","objectId":3000019,"class":"Address"},"objectId":3000020,"department":{"manager":{"objectId":3000015,"name":"EmployeeA1","class":"Manager"},"objectId":3000014,"name":"DepartmentA1","company":{"objectId":3000000,"name":"CompanyA","class":"Company"},"class":"Department"},"age":20,"name":"EmployeeA2","class":"Employee","fullTime":true},{"address":{"address":"Employee Street, AVille","objectId":3000019,"class":"Address"},"objectId":3000021,"department":{"manager":{"objectId":3000015,"name":"EmployeeA1","class":"Manager"},"objectId":3000014,"name":"DepartmentA1","company":{"objectId":3000000,"name":"CompanyA","class":"Company"},"class":"Department"},"age":30,"name":"EmployeeA3","class":"Employee","fullTime":false}],'executionTime':'\E\d{4}\.\d{2}\.\d{2} \d{2}\:\d{2}\:\:\d{2}\Q'}/;

like($res, $expected_re, "and it has the right content");

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
ok($res->[1]->getFullTime, "with the right fields - Bool");

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
[
    'EmployeeB1',
    '40'
],
[
    'EmployeeB2',
    '50'
],
[
    'EmployeeB3',
    '60'
]
];

is_deeply($res, $exp_res, "With the right fields")
    or diag($t->url, explain $res), diag $t->show_constraints;

$exp_res = [
[
    'EmployeeA1',
    '10'
],
[
    'EmployeeA2',
    '20'
],
[
    'EmployeeA3',
    '30'
]
];

is_deeply($t->results,  $exp_res, "And ditto for results") or diag($t->url, explain $res);

$exp_res = [['EmployeeA2',20]];

is_deeply($t->results(size => 1, start => 1), $exp_res, "And it handles start and size");

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

my $loaded;
lives_ok {$loaded = $module->load_query(source_file => "t/data/loadable_query.xml")} 
    "Can load a query";

$exp_res = 
[
   [
     'EmployeeA1',
     'DepartmentA1'
   ]
];

$res = $loaded->results;
is_deeply($res, $exp_res, "Can get results for queries loaded from xml")
    or diag(explain $res);

