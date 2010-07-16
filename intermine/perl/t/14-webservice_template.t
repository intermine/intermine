#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 22;
use Test::Exception;
use Test::MockObject::Extends;

use InterMine::Model;

### Setting up

my $module = 'InterMine::WebService::Service::TemplateService';
my $url = 'http://www.fakeurl.org/query/service';
my @methods = qw/new 
                 get_templates
                 get_template
                 search_for 
                 get_result
                 /;

my $test_templates = '../api/test/resources/default-template-queries.xml';

my $fake_content;
open my $GFH, '<', $test_templates or die "Cannot open $test_templates, $!";
$fake_content .= $_ for <$GFH>;
close $GFH or die "Cannot close $test_templates, $!";

my $fake_response = Test::MockObject::Extends->new;
$fake_response->set_true('is_error')
    ->mock(content => sub {return $fake_content})
    ->mock(status_line => sub {return 'mock http error'})
;


my @empty_array = ();

my $exp_req_params = {
          'constraint1' => 'Employee.name',
          'format' => 'tab',
          'name' => 'fake_search',
          'op1' => '='
        };
my $exp_multi_req_params =  {
          'constraint2' => 'Employee.age',
          'name' => 'fake_search',
          'value2' => '55',
          'constraint1' => 'Employee.age',
          'value1' => '35',
          'format' => 'tab',
          'op2' => '!=',
          'op1' => '>'
        };


### Tests

use_ok($module); # Test 1
can_ok($module, @methods); # Test 2

my $service = new_ok($module => [$url], 'Service'); # Test 3
isa_ok($service, 'InterMine::WebService::Core::Service', 'Inherits ok'); # Test 4

##### Test _make_templates_from_xml
my $model = '../objectstore/model/testmodel/testmodel_model.xml';
my $ms = $service->{model_service};
$ms = Test::MockObject::Extends->new($ms);
$ms->mock(get_model => sub {return InterMine::Model->new(file => $model)});

### Test get_templates

# isolate execute_request, which is tested in webservice_core_service.t
$service = Test::MockObject::Extends->new($service);
$service->mock(execute_request => sub {return $fake_response});

# when we already have templates, we return them, instead of fetching a new list

my $test_array = [qw/one two three/];
$service->{'templates'} = $test_array;

is_deeply([$service->get_templates], $test_array, # Test 5
	  'Retrieves cached lists of templates'); 
delete $service->{'templates'};

# Catch communication errors

throws_ok(sub {$service->get_templates}, # Test 6
	 qr/Fetching templates failed.*mock http error/,
	 'Catches http error ok');

# Check for successful queries

$fake_response->set_false('is_error'); 
my ($template) = $service->get_templates;
isa_ok($template, 'InterMine::Template', 'Returned template'); # Test 7

is_deeply($template, $service->{templates}->[0], 'Stores templates ok'); # Test 8


### Test search_for

throws_ok( sub {$service->search_for}, qr/You need a keyword/, # Test 9
	   'Catches search for without a keyword');

my @kwijibobs = $service->search_for('kwijibob');
is_deeply(\@kwijibobs, \@empty_array, 'Return empty array for failed search'); # Test 10

my ($found) = $service->search_for('ByName');
is_deeply($found, $template, "Returns one result from search ok"); # Test 11

my @founds = grep {$_->isa('InterMine::Template')} $service->search_for('employee');
ok(@founds > 1, 'Can return multiple results from search ok'); # Test 12

### Test get_template

throws_ok(sub {$service->get_template}, qr/You need a name/, # Test 13
	  'Catches get_template without a name');
is($service->get_template('kwijibob'), undef,                # Test 14
          'Return undef for unfound template'); 
is_deeply($service->get_template('employeeByName'), $template,             # Test 15
	  'Can get a template by name');

my $multi_cons = $service->get_template('employeesOfACertainAge');

$service->mock(get_templates => sub { # mock template to return ambiguous results
    my @t = @{shift->{templates}};
    $_->set_name('fake_search') for (@t);
    return @t;
   }
);

is($service->get_template('fake_search'), undef, 'Return undef for multiple results'); # Test 16
$service->unmock('get_templates');

### Test get_result


throws_ok( sub {$service->get_result}, # Test 17
	   qr/get_result needs a valid InterMine::Template/,
	   'Catches lack of template');

$service->mock(execute_request => sub {my ($self, $req) = @_; return $req});

lives_ok(sub {$service->get_result($template)}, # Test 18
	 'Happily processes a valid template'); 
my $ret = $service->get_result($template);
isa_ok($ret, 'InterMine::WebService::Core::Request', 'Results request');  # Test 19
is($ret->get_url, $url.'/template/results', 'Constructs url ok');         # Test 20
is_deeply({$ret->get_parameters}, $exp_req_params, 'Sets parameters ok'); # Test 21
$ret = $service->get_result($multi_cons);
is_deeply({$ret->get_parameters}, $exp_multi_req_params,                  # Test 22
	  'Sets parameters ok for templates with more than one constraint');
