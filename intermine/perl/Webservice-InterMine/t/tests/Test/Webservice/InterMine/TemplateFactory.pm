package Test::Webservice::InterMine::TemplateFactory;

use strict;
use warnings;

use base ('Test::Class');
use Test::More;
use Test::Exception;
use Test::MockObject;
use InterMine::Model;

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
    my $model = InterMine::Model->new(
	file => 't/data/testmodel_model.xml'
    );
    return $model;
}

sub startup : Test(startup => 1) {
    my $test = shift;
    use_ok($test->class);
    my $fake_Template = Test::MockObject->new;
    $fake_Template->fake_module(
	'Webservice::InterMine::Query::Template',
	new => sub {
	    return $fake_Template;
	},
    );
    $fake_Template->set_isa('Webservice::InterMine::Query::Template');
    $fake_Template->mock(
	name => sub {
	    return $next_char++;
	},
    );
    $test->{template} = $fake_Template;


}

sub construction : Test(7) {
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
    my $bad_xml_error = qr/Can't find any template strings in the xml I was passed/;
    throws_ok(
	sub {$test->class->new(
	    service => $test->service,
	    model => $test->model,
	)},
	$missing_source_error,
	"... and catches constructing without source",
    );
    throws_ok(
	sub {$test->class->new(
	    source_file => $test->nonexistent_file,
	    service       => $test->service,
	    model         => $test->model,
	)},
	$bad_arg_error,
	"... and catches constructing with nonexistent file",
    );
    throws_ok(
	sub {$test->class->new(
	    source_string => 'foo',
	    service       => $test->service,
	    model         => $test->model,
	)},
	$bad_xml_error,
	"... and catches constructing with a bad string",
    );
    throws_ok(
	sub {
	    $test->class->new(
		[$test->service,
		$test->model,
		$test->source_file,
	     ],
	    );
	},
	$bad_xml_error,
	"... and catches when calling new in the one arg style with a file",
    );
}

sub methods : Test {
    my $test = shift;
    my @methods = (qw/get_template_by_name get_templates get_template_names/);
    can_ok($test->class, @methods);
}

sub get_templates : Test {
    my $test = shift;
    my $obj  = $test->{object};
    my @templates = $obj->get_templates;
    is(scalar(@templates), 12, "Gets the right number of templates");
}

sub get_template_by_name : Test {
    my $test = shift;
    my $obj  = $test->{object};
    my $t    = $obj->get_template_by_name('K');
    is($t, $test->{template},"Stores and retrieves a template by name");
}

sub get_template_names : Test {
    my $test = shift;
    my $obj  = $test->{object};
    my @names = ('A' .. 'L');
    is_deeply(
	[sort $obj->get_template_names], \@names,
	"Stores and retrieves a list of template names",
    );
}

1;



