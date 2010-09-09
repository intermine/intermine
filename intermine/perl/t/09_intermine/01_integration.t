use strict;
use warnings;

use lib 't/tests';

use Test::More tests => 40;
use Test::MockObject::Extends;
use Test::Exception;
use HTTP::Response;
use IO::File;
use YAML::Syck;

sub slurp {
    my $file = shift;
    return join('', IO::File->new($file, 'r')->getlines);
}

my $module = 'InterMine';

my $results_file = 't/data/mock_content_results';

my $model     = slurp('t/data/testmodel_model.xml');
my $results   = slurp($results_file);
my $templates = slurp('t/data/default-template-queries.xml');

my $fake_lwp = Test::MockObject::Extends->new('LWP::UserAgent');
$fake_lwp->fake_module(
    'LWP::UserAgent',
    new => sub {
	return $fake_lwp;
    },
);
$fake_lwp->mock(
    get => sub {
	my $self = shift;
	my $uri  = shift;
	my ($code, $msg, $head, $content) = (200, 'OK', [foo => 'bar']);
	if ($uri =~ m!templates/xml!) {
	    $content = $templates;
	}
	elsif ($uri =~ m!/model\?!) {
	    $content = $model;
	}
	elsif ($uri =~ m!version!) {
	    $content = 2;
	}
	elsif ($uri =~ m!/results\?!) {
	    $content = $results;
	}
	my $r = HTTP::Response->new( $code, $msg, $head, $content );
	return $r;
    },
);
my $fake_IOSock = Test::MockObject::Extends->new();
$fake_IOSock->set_isa('Net::HTTP');
$fake_IOSock->fake_module(
    'Net::HTTP',
    new => sub {
	delete $fake_IOSock->{io};
	$fake_IOSock->{io} = IO::File->new($results_file, 'r');
	return $fake_IOSock;
    },
);
$fake_IOSock->mock(
    write_request => sub {},
);
$fake_IOSock->mock(
    getline => sub {
	my $self = shift;
	return $self->{io}->getline;
    },
);
$fake_IOSock->mock(
    close => sub {
	my $self = shift;
	$self->{io}->close;
    },
);

my $url = 'FAKEURL';
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

lives_ok(
    sub {$res = $q->results},
    "Queries for results",
);

is(ref $res, 'ARRAY', "And it is an arrayref");

is(ref $res->[0], 'ARRAY', "An array of arrays in fact");

is($res->[1][3], "Chédin S", "With the right fields")
    or diag(explain $res);

lives_ok(
    sub {$res = $q->results(as => 'hashrefs')},
    "Queries for results as hashes",
);

is($res->[1]->{'Employee.address.address'}, "Chédin S", "with the right fields");

my $test_role = 'Test::InterMine::FooBar';
my $q_roled;
lives_ok(
    sub {$q_roled = InterMine->new_query(with => [$test_role])},
    "Can make a query with a role",
);

is($q_roled->foo, "bar", "And it does what it's meant to");


my $role = 'InterMine::ResultIterator::Role::HTMLTableRow';
my $ri;
lives_ok(
    sub {$ri = $q->results_iterator(with => [$role, $test_role])},
    "Gets a results iterator with a role",
);
is($ri->foo, 'bar', "And it has one of the methods");

my $row = "<tr><td>S000000001</td><td>YAL001C</td><td>10531351</td><td>Rubbi L</td><td>J Biol Chem</td><td>1999</td><td>Saccharomyces cerevisiae</td></tr>";
is($ri->html_row, $row, "And it has the other");

my $t;
lives_ok(
    sub {$t = $module->template('employeesOfACertainAge');},
    "Gets a template ok",
);

isa_ok($t, 'InterMine::Query::Template', "The template");

is($t->editable_constraints, 2, "And it has 2 editable constraints");

lives_ok(
    sub {$res = $t->results_with(valueA => 'foo');},
    "Runs results with ok",
);

is($res->[1][3], "Chédin S", "With the right fields");

is($t->results->[1][3],  "Chédin S", "And ditto for results");

$role = 'InterMine::Query::Roles::HTMLTable';
lives_ok(
    sub {$t = $module->template('employeeByName', with => [$role, $test_role]);},
    "Gets a template with a role ok",
);
is($t->foo, 'bar', "Does test role ok");
like(
    $t->results_as_html_table,
    qr|^<table>(?:<tr>(?:<td>.*</td>)*</tr>)*</table>$|,
    "Makes a table of results ok"
);
$role = 'InterMine::Query::Roles::WriteOutYaml';
$t = $module->template('employeeByName', with => [$role]);
my $out_buffer;
open(my $out_handle, '>', \$out_buffer) or die $!;
lives_ok(
    sub{$t->dump_yaml_to_file(file => $out_handle);},
    "lives dumping yaml",
);
my $data = Load($out_buffer);
is_deeply($data, $res, "Yamlises, and back, ok");
