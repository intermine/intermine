package Test::Webservice::InterMine::Query::Saved;

use base ('Test::Webservice::InterMine::Query::Core');
use Test::More;
use Test::XML;
use Test::MockObject;
sub class {'Webservice::InterMine::Query::Saved'}
sub savedquery_xml {q|<saved-query name="Query 1" date-created="1160647639444">
<query name="Query 1" model="testmodel" view="Employee.name Employee.age" constraintLogic="A and B and C">
     <constraint code="A" op="=" path="Employee.age" value="18" />
     <constraint code="B" op="=" path="Employee.department.name" value="Catering*" />
     <constraint path="Employee.department.company.name" op="ONE OF" code="C" >
       <value>The Sandwich Van</value>
       <value>Prêt-à-Manger</value>
       <value>The Cambridge Blue</value>
     </constraint>
   </query>
</saved-query>|}
sub exp_xml {q|<saved-query date-created="1160647639444" name="Query 1">
   <query constraintLogic="A and B and C" model="testmodel" name="Query 1" sortOrder="Employee.name asc" view="Employee.name Employee.age">
     <constraint code="A" op="=" path="Employee.age" value="18"/>
     <constraint code="B" op="=" path="Employee.department.name" value="Catering*"/>
     <constraint path="Employee.department.company.name" op="ONE OF" code="C" >
       <value>The Sandwich Van</value>
       <value>Prêt-à-Manger</value>
       <value>The Cambridge Blue</value>
     </constraint>
   </query>
 </saved-query>|}
sub exp_url {q|FAKEROOTFAKEPATH|}

sub exp_head {
    return (
	name  => "Query 1",
	'date-created' => "1160647639444",
    );
}

sub args {
    my $test = shift;
    return (
	model => $test->model,
	source_string => $test->savedquery_xml,
	service => $test->{service},
    );
}

sub test_paths {
    my $test = shift;
    my @paths = $test->SUPER::test_paths;
    return (@paths,
	    'Employee.department.company.name');
}

sub logic_string1 {'A and B and C and D and E and F'}
sub logic_string2 {'(D or E) and F'}

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
	version => sub {
	    return 2;
	},
    );
    $service->mock(
	QUERY_PATH => sub {
	    return 'FAKEPATH';
	},
    );
    $service->mock(
	get_results_iterator => sub {
	    return $test->{iterator};
	},
    );
    $test->{service} = $service;

    my $iterator = Test::MockObject->new;
    $iterator->mock(
	all_lines => sub {
	    my $self = shift;
	    return @_, @_, @_; #repeated so we get a list back
	},
    );
    $test->{iterator} = $iterator;
    $test->SUPER::startup;
}


sub _methods : Test(2) {
    my $test = shift;
    $test->SUPER::_methods;
    my @methods = (
	qw/to_xml source_string source_file url service
	   head insertion date results results_iterator/
    );
    can_ok($test->class, @methods);
}

sub _inheritance : Test {
    my $test = shift;
    isa_ok($test->class, 'Webservice::InterMine::Query::Core');
}

sub to_xml : Test {
    my $test = shift;
    my $obj = $test->{object};
    is_xml(
	$obj->to_xml,
	$test->exp_xml,
	"Can serialise to xml",
    );
}

sub head : Test(2) {
    my $test = shift;
    my $obj = $test->{object};
    my %exp_head = $test->exp_head;
    is_deeply(
	$obj->head, \%exp_head,
	"Gets the head correctly",
    );
    $obj->date(123456789);
    $exp_head{'date-created'} = 123456789;
    is_deeply(
	$obj->head, \%exp_head,
	"Gets the head correctly with changes",
    );
}

sub sort_order_initial_state : Test {
    my $test = shift;
    my $obj = $test->{object};
    is(
	$obj->sort_order, 'Employee.name asc',
	"Sets the sort order correctly",
    );
}

sub url : Test {
    my $test = shift;
    my $obj  = $test->{object};
    is($obj->url, $test->exp_url, "Makes a good url");
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

1;

