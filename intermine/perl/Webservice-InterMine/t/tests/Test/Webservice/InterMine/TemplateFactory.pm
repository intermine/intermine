package Test::Webservice::InterMine::TemplateFactory::TestModel;

use strict;
use warnings;

use base ('Test::Class');
use Test::More;
use Test::Exception;
use Test::MockObject;
use InterMine::Model::TestModel;

sub class {'Webservice::InterMine::TemplateFactory'}
sub source_file {'t/data/default-template-queries.xml'}
sub source_string {
    my $test = shift;
    my $file = $test->source_file;
    open(my $FH, '<', $file) or die "Danger Will Robinson!";
    my $string = join('', <$FH>);
    return $string;
}
sub nonexistent_file {'t/data/thisfiledoesntexist'}

my $next_char = 'A';

sub service {
    my $service = Test::MockObject->new;
    $service->set_isa('Webservice::InterMine::Service');
    return $service;
}

sub model {
    return InterMine::Model::TestModel->instance;
}

sub startup : Test(startup => 1) {
    my $test = shift;
    use_ok($test->class);
}

sub construction : Test(9) {
    my $test = shift;
    my $obj  = new_ok($test->class, [
        source_file   => $test->source_file,
        service       => $test->service,
        model         => $test->model,
    ]);

    $test->{object} = $obj;

    new_ok($test->class, [
        source_string => $test->source_string,
        service       => $test->service,
        model         => $test->model,
    ]);

    new_ok(
	$test->class, [
	    [
		$test->service,
		$test->model,
		$test->source_string,
	    ]
	]
    );
    my $missing_source_error = qr/source xml must be passed to a TemplateFactory as either a string or a file/;
    my $bad_arg_error = qr/does not pass the type constraint because:.*should be a file/;
    my $bad_xml_error = qr/Error parsing template XML: 'foo'/;

    throws_ok {$test->class->new(service => $test->service, model => $test->model,)}
        $missing_source_error,
	    "... and catches constructing without source";

    throws_ok {$test->class->new(
            source_file => $test->nonexistent_file, 
            service => $test->service,
	        model         => $test->model,
	    )} $bad_arg_error,
        "... and catches constructing with nonexistent file";

    throws_ok {$test->class->new(
            source_string => 'foo',
            service       => $test->service,
            model         => $test->model,
        )} $bad_xml_error,
        "... and catches constructing with a bad string";

    my $empty_factory;
    lives_ok {$empty_factory = $test->class->new(
            source_string => '<templates></templates>',
            service       => $test->service,
            model         => $test->model,
        );} "Is happy to parse an empty template list";

    is($empty_factory->get_template_count, 0, "And it reports the template number correctly");

    throws_ok(
        sub {$test->class->new([$test->service, $test->model, $test->source_file]);},
        qr!Error parsing template XML: 't/data/default-template-queries.xml'!,
        "... and catches when calling new in the one arg style with a file",
    );
}

sub methods : Test {
    my $test = shift;
    my @methods = (qw/get_template_by_name get_templates get_template_names/);
    can_ok($test->class, @methods);
}

sub get_templates : Test(2) {
    my $test = shift;
    my $obj  = $test->class->new(
        source_file   => $test->source_file,
        service       => $test->service,
        model         => $test->model,
    );
    my @templates = $obj->get_templates;
    is(scalar(@templates), 15, "Gets the right number of templates");
    is($obj->_get_parsed_count, 15, "Getting templates means they get parsed");
}

sub get_template_by_name : Test(4) {
    my $test = shift;
    my $obj  = $test->class->new(
        source_file   => $test->source_file,
        service       => $test->service,
        model         => $test->model,
    );
    my $t = $obj->get_template_by_name('employeeByName');

    isa_ok($t, 'Webservice::InterMine::Query::Template');
    is($t->title, "View all the employees with certain name", "Retrieves a template");
    is($obj->_get_parsed_count, 1, "Only this template has been parsed");
    is($obj->get_template_count, 15, "But all templates have been found");

}

sub get_template_names : Test(2) {
    my $test = shift;
    my $obj  = $test->class->new(
        source_file   => $test->source_file,
        service       => $test->service,
        model         => $test->model,
    );
    my $exp = [
        'InnerInsideOuter',
        'ManagerLookup',
        'MultiValueConstraints',
        'RangeQueries',
        'SortOrderNotInView',
        'SubClassContraints',
        'SwitchableConstraints',
        'UneditableConstraints',
        'convertContractorToEmployees',
        'convertEmployeeToManager',
        'convertEmployeesToAddresses',
        'employeeByName',
        'employeesFromCompanyAndDepartment',
        'employeesOfACertainAge',
        'employeesOverACertainAgeFromDepartmentA'
    ];

    my $got = [sort $obj->get_template_names];
    is_deeply($got, $exp, "Stores and retrieves a list of template names") or diag(explain $got);
    is($obj->_get_parsed_count, 0, "Getting names doesn't parse the templates");
}

1;



