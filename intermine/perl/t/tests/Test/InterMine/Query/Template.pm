package Test::InterMine::Query::Template;

use base ('Test::InterMine::Query::Core');
use Test::MockObject;
use Test::MockObject::Extends;
use Test::More;
use Test::XML;
use Test::Exception;
use Data::Dumper;

sub class {'InterMine::Query::Template'}
sub template_xml {q|
<template name="employeesFromCompanyAndDepartment" description="View all the employees that work within a certain department of the specified company" >
     <query name="employeesFromCompanyAndDepartment" model="testmodel" view="Employee.name Employee.age" constraintLogic="A and B">
     <constraint description="" identifier="" path="Employee.department.company.name" op="=" value="CompanyA" code="A" />
     <constraint description="" identifier="" path="Employee.department.name" op="LIKE" value="FOO" code="B" editable="true" switchable="on"/>
   </query>
</template>
|}
sub exp_xml {q|<template comment="" longDescription="" name="employeesFromCompanyAndDepartment" title="View all the employees that work within a certain department of the specified company">
   <query constraintLogic="A and B" model="testmodel" name="employeesFromCompanyAndDepartment" sortOrder="Employee.name asc" view="Employee.name Employee.age">
     <constraint description="" identifier="" code="A" editable="false" op="=" path="Employee.department.company.name" switchable="locked" value="CompanyA"/>
     <constraint description="" identifier="" code="B" editable="true" op="LIKE" path="Employee.department.name" switchable="on" value="FOO"/>
   </query>
 </template>

|}

sub exp_shown_con {
    return q|B) Employee.department.name LIKE "FOO" (on)|;
}

sub exp_head {
    return (
	name  => "employeesFromCompanyAndDepartment",
	title => "View all the employees that work within a certain department of the specified company",
	longDescription => '',
	comment => '',
    );
}
sub exp_url {'FAKEROOTFAKEPATH?constraint1=Employee.department.name&value1=FOO&format=tab&name=employeesFromCompanyAndDepartment&code1=B&op1=LIKE'}

sub args {
    my $test = shift;
    return (
	model => $test->model,
	source_string => $test->template_xml,
	service => $test->{service},
    );
}
sub extra_constraint_args {(is_editable => 1)}
sub test_paths {
    my $test = shift;
    my @paths = $test->SUPER::test_paths;
    return (@paths, 'Employee.department.company.name');
}

sub logic_string1 {'A and B and C and D and E'}
sub logic_string2 {'(C or D) and E'}

sub startup {
    my $test = shift;

    my $service = Test::MockObject->new;
    $service->fake_module(
	'InterMine::Service',
	new => sub {
	    return $service;
	},
    );
    $service->set_isa('InterMine::Service');
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
	TEMPLATEQUERY_PATH => sub {
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
	qw/to_xml source_string source_file url
	  results_with service_root templatequery_path
	  editable_constraints show_constraints
	  comment title head insertion/
    );
    can_ok($test->class, @methods);
}

sub _inheritance : Test {
    my $test = shift;
    isa_ok($test->class, 'InterMine::Query::Core');
}

sub head : Test(2) {
    my $test = shift;
    my $obj = $test->{object};
    my %exp_head = $test->exp_head;
    is_deeply(
	$obj->head, \%exp_head,
	"Gets the head correctly",
    );
    $obj->comment("a very nice template");
    $obj->description("something kind of templatey");
    $exp_head{comment} = "a very nice template";
    $exp_head{longDescription} = "something kind of templatey";
    is_deeply(
	$obj->head, \%exp_head,
	"Gets the head correctly with changes",
    );
}

sub to_xml : Test {
    my $test = shift;
    my $obj = $test->{object};
    is_xml(
	$obj->to_xml, $test->exp_xml,
	"Serialises to xml ok",
    );
}
sub sort_order_initial_state : Test {
    my $test = shift;
    my $obj = $test->{object};
    is(
	$obj->sort_order->to_string, 'Employee.name asc',
	"Sets the sort order correctly",
    );
}

sub view : Test(7) {
    my $test = shift;
    my $obj  = $test->{object};
    my @initial_view = ('Employee.name', 'Employee.age');
    is_deeply(
	[$obj->views], \@initial_view, "Has a good initial view",
    );
    $obj->clear_view;
    $test->SUPER::view;
}

sub url : Test {
    my $test = shift;
    my $obj = $test->{object};
    is(
	$obj->url, $test->exp_url,
	"Makes a good url",
    );
}

sub results : Test(4) {
    my $test = shift;
    my $obj  = $test->{object};

    is(
	$obj->results(as => 'string'),
	"string\nstring\nstring",
	"returns new-line joined string for string results",
    );
    is_deeply(
	$obj->results(as => 'arrayref'),
	['arrayref', 'arrayref', 'arrayref'],
	"returns array ref of arrayrefs for arrayref results",
    );
    is_deeply(
	$obj->results(as => 'hashref'),
	['hashref', 'hashref', 'hashref'],
	"returns array ref of hashrefs for hashref results",
    );
    is_deeply(
	$obj->results(),
	['arrayref', 'arrayref', 'arrayref'],
	"Default as per arrayref",
    );
}

sub results_with : Test(14) {
    my $test    = shift;
    my $obj     = $test->{object};
    my $exp_xml = $test->exp_xml;
    my $before  = $obj->show_constraints;
    $obj = Test::MockObject::Extends->new($obj);
    $obj->mock(
	results => sub {
	    return [@_];
	},
    );
    my $results;
    lives_ok(
	sub {$results = $obj->results_with();},
	"Runs results_with with no args OK",
    );
    is_xml(
	$results->[0]->to_xml,
	$exp_xml,
	"The xml comes out as expected",
    );
    lives_ok(
	sub {$results = $obj->results_with(valueB => 'BAR');},
	"Runs results_with OK with a value",
    );
    my $after = $obj->show_constraints;
    is($after, $before, "and does not change the obj's constraints");
    $exp_xml =~ s/FOO/BAR/;
    is_xml(
	$results->[0]->to_xml,
	$exp_xml,
	"The xml comes out as expected",
    );
    is_deeply(
	[$results->[1], $results->[2]], ['as', undef], "the format arg is correct",
    );
    lives_ok(
	sub {$results = $obj->results_with(opB => 'CONTAINS');},
	"runs results with ok with an operator",
    );
    $exp_xml =~ s/BAR/FOO/;
    $exp_xml =~ s/LIKE/CONTAINS/;
    is_xml(
	$results->[0]->to_xml,
	$exp_xml,
	"The xml comes out as expected",
    );
   lives_ok(
	sub {$results = $obj->results_with(valueB => 'QUUX', opB => 'CONTAINS');},
	"runs results with ok with an operator and a value",
    );
    $exp_xml =~ s/FOO/QUUX/;
    is_xml(
	$results->[0]->to_xml,
	$exp_xml,
	"The xml comes out as expected",
    ) or diag($exp_xml);

    $exp_xml =~ s/QUUX/ZOP/;
    lives_ok(
       sub {$results = $obj->results_with(valueB => 'ZOP', opB => 'CONTAINS', as => 'strings');},
	"runs results with ok with an operator and a value and a format",
    );
    is(
	$results->[2], 'strings', "the format arg is correct",
    );
    is_xml(
	$results->[0]->to_xml,
	$exp_xml,
	"The xml comes out as expected",
    ) or diag($exp_xml);

    throws_ok(
	sub {$obj->results_with(valueA => 'foo')},
	qr/You can only change values and operators for editable constraints/,
	"Catches attempts to apply values to non editable constraints",
    );
}

sub editable_constraints : Test {
    my $test = shift;
    my $obj  = $test->{object};
    is($obj->editable_constraints, 1, "Parses editable attribute correctly");
}

sub show_constraints : Test {
    my $test = shift;
    my $obj  = $test->{object};
    is($obj->show_constraints, $test->exp_shown_con, "Reports constraints correctly");
}

sub save : Test {
    local $TODO = "saving currently unimplemented";
}

1;

