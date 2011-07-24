package Test::Webservice::InterMine::Query::Scripted;

use base ('Test::Webservice::InterMine::Query::Core');
use Test::MockObject;
use Test::More;
sub class {'Webservice::InterMine::Query::Scripted'}
sub query_xml {q|<query name="employeeByName" model="testmodel" view="Employee.name Employee.age">
        <constraint path="Employee.name" op="=" value="" description="" identifier="Employee.name" editable="true" code="A" />
      </query>|}
sub script {
    my $script = <<'ENDSCRIPT';
#!/usr/bin/perl

use Webservice::InterMine 'FAKEROOT';
my $query = Webservice::InterMine->new_query;

$query->add_view( 'Employee.name', 'Employee.age' );

$query->add_constraint(
    value => '',
    path  => 'Employee.name',
    op    => '=',
    code  => 'A',
);

my $results = $query->results;
ENDSCRIPT
    return $script;
}
sub args {
    my $test = shift;
    return (
	model => $test->model,
	source_string => $test->query_xml,
	service => $test->{service},
    );
}

sub logic_string1 {'A and B and C and D'}
sub logic_string2 {'(B or C) and D'}

sub startup {
    my $test = shift;

    my $service = Test::MockObject->new;
    $service->fake_module(
	'Webservice::InterMine::Service',
	new => sub {
	    return $service;
	},
    );
    $service->set_isa('Webservice::InterMine::Service');
    $service->mock(
	model => sub {
	    return $test->model;
	},
    );
    $service->mock(
	get_results_iterator => sub {
	    my $self = shift;
	    return @_;
	},
    );
    $service->mock(
	root => sub {
	    return 'FAKEROOT';
	},
    );
    $service->mock(
	QUERY_PATH => sub {
	    return 'FAKEPATH';
	},
    );
    $test->{service} = $service;
    $test->SUPER::startup;
}

sub _methods : Test(2) {
    my $test = shift;
    $test->SUPER::_methods;
    my @methods = (qw/to_script source_string source_file/);
    can_ok($test->class, @methods);
}

sub _inheritance : Test {
    my $test = shift;
    isa_ok($test->class, 'Webservice::InterMine::Query::Core');
}

sub sort_order_initial_state : Test {
    my $test = shift;
    my $obj = $test->{object};
    is(
	$obj->sort_order, 'Employee.name asc',
	"Sets the sort order correctly",
    );
}

sub view : Test(9) {
    my $test = shift;
    my $obj  = $test->{object};
    my @initial_view = ('Employee.name', 'Employee.age');
    is_deeply(
	[$obj->views], \@initial_view, "Has a good initial view",
    );
    $obj->clear_view;
    $test->SUPER::view;
}

sub to_script : Test {
    my $test = shift;
    my $obj  = $test->{object};
    is(
	$obj->to_script, $test->script,
	"Can make a script correctly",
    );
}
1;

