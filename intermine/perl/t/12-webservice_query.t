#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 11;
use Test::Exception;
use Test::MockObject::Extends;

### Setting up

my $module = 'InterMine::WebService::Service::QueryService';
my $url = 'http://www.fakeurl.org/query/service';
my @methods = (qw/new get_result get_result_table/);

my $fake_content = 
q/FBal0177077	zwilch[1229]	hypomorph	FBgn0061476	zwilch
FBal0128957	zf30C[EP518]	hypomorph	FBgn0022720	zf30C
FBal0128958	zf30C[EP2228]	hypomorph	FBgn0022720	zf30C
FBal0018859	zen[5]	loss of function, hypomorph	FBgn0004053	zen
FBal0018855	zen[1]	hypomorph	FBgn0004053	zen
FBal0018840	z[ae(bx)]	amorph, loss of function, hypomorph	FBgn0004050	z
FBal0018834	z[a]	amorph, loss of function, hypomorph	FBgn0004050	z
FBal0095115	y[a77]	hypomorph	FBgn0004034	y
FBal0036046	y[D91]	hypomorph	FBgn0004034	y
FBal0018616	y[3d]	hypomorph	FBgn0004034	y/;

my $fake_response = Test::MockObject::Extends->new;
$fake_response->set_false('is_success')
    ->mock(content => sub {return $fake_content})
    ->mock(status_line => sub {return 'mock http error'})
    ;

### Tests

use_ok($module); # Test 1
can_ok($module, @methods); # Test 2

my $service = new_ok($module => [$url]); # Test 3
isa_ok($service, 'InterMine::WebService::Core::Service', 'Inherits ok'); #Test 4

is($service->get_url, $url.'/query/results', 'Sets the url ok'); # Test 5

### Test get_result
# isolate execute_request, which is tested in webservice_core_service.t
$service = Test::MockObject::Extends->new($service);
$service->mock(execute_request =>
	       sub {my ($self, $req) = @_;
		    my $url = URI->new($req->get_url);
		    $url->query_form($req->get_parameters);
		    return $url;}
    );

throws_ok(sub {$service->get_result}, qr/No query passed/, 'Catches query-less queries'); # Test 6

my $query = q(
   <query name="" model="genomic" view="Organism.name Organism.taxonId" sortOrder="Organism.name"/>
);
my $expected_url = 'http://www.fakeurl.org/query/service/query/results?format=tab&query=%0A+++%3Cquery+name%3D%22%22+model%3D%22genomic%22+view%3D%22Organism.name+Organism.taxonId%22+sortOrder%3D%22Organism.name%22%2F%3E%0A&start=0&size=100';

is($service->get_result($query), $expected_url, 'get_result produces good urls'); # Test 7

### Now isolate get_result and test get_result_table
$service->mock(get_result => sub {return $fake_response});

# first failure case
throws_ok( sub {$service->get_result_table}, qr/request failed with error.*mock http error.*\Q$fake_content\E/s, 'get_result_table catches http error'); # Test 8

# And success case
$fake_response->set_true('is_success');
my @returned = $service->get_result_table;
ok(@returned == 10, 'Returns the correct number of rows'); # Test 9
my $expected_row_5 = [
            'FBal0018840',
            'z[ae(bx)]',
            'amorph, loss of function, hypomorph',
            'FBgn0004050',
            'z'
    ];
is_deeply($returned[5], $expected_row_5, 'Formats the result table correctly'); # Test 10

$service->unmock('get_result');

throws_ok(sub {$service->get_count}, qr/No query passed/, 'Catches query-less queries'); # Test 11
