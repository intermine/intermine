use strict;
use warnings;

use Test::More tests => 33;
use Test::Exception;
my $module = 'InterMine';

my $url = 'localhost:8080/intermine-test/service';
my @view = ('Employee.name', 'Employee.age', 'Employee.fullTime',
	    'Employee.address.address', 'Employee.department.name',
	    'Employee.department.company.name',
	    'Employee.department.manager.name',
	);
use_ok($module, ($url));

isa_ok($module->get_service, 'InterMine::Service', "The service it makes");

is($module->get_service->version, 2, "Service version is correct");
isa_ok($module->get_service->model, 'InterMine::Model', "The model the service makes");
my $q;
lives_ok(sub {$q = $module->new_query}, "Makes a new query ok");
isa_ok($q, 'InterMine::Query', "The query");

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
	    op   => 'IN',
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

#diag($q2->url, "=>\n" , $q2->results(as => 'string'));

lives_ok(
    sub {$res = $q->results},
    "Queries for results",
) or diag($q->url);

is(ref $res, 'ARRAY', "And it is an arrayref");

is(ref $res->[0], 'ARRAY', "An array of arrays in fact");

is($res->[1][1], "20", "With the right fields - Int") or diag(explain $res);;
is($res->[1][3], "Employee Street, AVille", "With the right fields - Str") or diag(explain $res);;

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

my $t;
lives_ok(
    sub {$t = $module->template('employeesOfACertainAge');},
    "Gets a template ok",
);

isa_ok($t, 'InterMine::Query::Template', "The template");

is($t->editable_constraints, 2, "And it has 2 editable constraints");

lives_ok(
    sub {$res = $t->results_with(valueA => 10);},
    "Runs results with ok",
) or diag($t->url);

my $exp_res = [
    [
    'EmployeeA2',
    '20'
  ],
  [
    'EmployeeA3',
    '30'
  ],
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

is_deeply($t->results,  $exp_res, "And ditto for results") or diag($t->url, explain $res);
