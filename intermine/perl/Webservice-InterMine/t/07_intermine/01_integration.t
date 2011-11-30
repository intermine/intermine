use strict;
use warnings;
use Carp qw(confess cluck);
BEGIN {
    eval "use YAML::Syck";
    $ENV{TEST_YAML} = ($@) ? 0 : 1;
}
$SIG{__DIE__} = \&Carp::confess;
$SIG{__WARN__} = \&Carp::cluck;

use lib 't/tests'; # for the test role FooBar
use Test::More tests => 44;
use Test::MockObject::Extends;
use Test::Exception;
use HTTP::Response;
use IO::File;
use InterMine::Model::TestModel;

sub slurp {
    my $file = shift;
    return join('', IO::File->new($file, 'r')->getlines);
}

my $module = 'Webservice::InterMine';

my $results_file = 't/data/mock_content_results';

my $model     = InterMine::Model::TestModel->instance->to_xml;
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
	my ($code, $msg, $head, $content) = (200, 'OK', [foo => 'bar', 'Content-Location' => $uri]);
	if ($uri =~ m!templates/xml!) {
	    $content = $templates;
	}
	elsif ($uri =~ m!/model!) {
	    $content = $model;
	}
	elsif ($uri =~ m!version!) {
	    $content = 4;
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
    getline => sub {
	my $self = shift;
	return $self->{io}->getline;
    },
);
$fake_IOSock->mock(write_request => sub { return 1; });
$fake_IOSock->mock(
    close => sub {
	my $self = shift;
	$self->{io}->close;
    },
);

my $url = 'fake.url/path';
my @view = ('Employee.name', 'Employee.age', 'Employee.fullTime');
use_ok($module, ($url));

isa_ok($module->get_service, 'Webservice::InterMine::Service', "The service it makes");

is($module->get_service->version, 4, "Service version is correct");
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

lives_ok(
    sub {$res = $q->results},
    "Queries for results",
);

is(ref $res, 'ARRAY', "And it is an arrayref");

is(ref $res->[0], 'Webservice::InterMine::ResultRow', "An array of result-rows in fact");

is($res->[1][2], -1.23, "With the right fields")
    or diag(explain $res);

lives_ok(
    sub {$res = $q->results(as => 'hashrefs')},
    "Queries for results as hashes",
);

is($res->[1]->{'Employee.fullTime'}, -1.23, "with the right fields");

my $test_role = 'Test::Webservice::InterMine::FooBar';
my $q_roled;
lives_ok(
    sub {$q_roled = Webservice::InterMine->new_query(with => [$test_role])},
    "Can make a query with a role",
);

is($q_roled->foo, "bar", "And it does what it's meant to");


my $role = 'Webservice::InterMine::ResultIterator::Role::HTMLTableRow';
my $ri;
lives_ok(
    sub {$ri = $q->results_iterator(with => [$role, $test_role])},
    "Gets a results iterator with a role",
);
is($ri->foo, 'bar', "And it has one of the methods");

my $row = "<tr><td>foo</td><td>bar</td><td>baz</td></tr>";
is($ri->html_row, $row, "And it has the other");

my $t;
lives_ok(
    sub {$t = $module->template('employeesOfACertainAge');},
    "Gets a template ok",
);

isa_ok($t, 'Webservice::InterMine::Query::Template', "The template");

is($t->editable_constraints, 2, "And it has 2 editable constraints");

lives_ok(
    sub {$res = $t->results_with(valueA => 'foo');},
    "Runs results with ok",
);

is($res->[1][2], -1.23, "With the right fields");

is($t->results->[1][2],  -1.23, "And ditto for results");

$role = 'Webservice::InterMine::Query::Roles::HTMLTable';
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

SKIP: {
    skip(
        "Need YAML::Syck to test YAML role",
        3,
    ) unless $ENV{TEST_YAML};

    $role = 'Webservice::InterMine::Query::Roles::WriteOutYaml';
    $t = $module->template('employeeByName', with => [$role]);
    my $out_buffer;
    lives_ok(
        sub{$out_buffer = $t->results_to_yaml();},
        "lives dumping yaml",
    );
    my $data;
    my $i = 1;
    lives_ok(
        sub {($data) = Load($out_buffer);},
        "Lives loading yaml",
    ) or diag(join "\n", map {$i++ . $_} split("\n", $out_buffer));

    my $res = $t->results(as => "arrayrefs");
    is_deeply($data, $res, "Yamlises, and back, ok");
}

my $loaded;
lives_ok {$loaded = $module->load_query(source_file => "t/data/loadable_query.xml")} 
    "Can load a query";

is_deeply([$loaded->views], ["Employee.name", "Employee.department.name"], "And it can parse it ok");

my $expected_out_xml = q!<saved-query name=""><query name="" model="testmodel" view="Employee.name Employee.department.name" sortOrder="Employee.name asc"><constraint value="20" path="Employee.age" code="A" op="&lt;"/></query></saved-query>!;
is($loaded->to_xml, $expected_out_xml);
