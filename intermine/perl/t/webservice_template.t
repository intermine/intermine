#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 9;
use Test::Exception;

BEGIN {
      use_ok('InterMine::WebService::Service::TemplateService');
}

my $module = 'InterMine::WebService::Service::TemplateService';
my $args_ref = ['http://preview.flymine.org/query/service',
		   'service_tests'];
my @methods = qw/new 
                 search_for 
                 get_templates 
                 _make_templates_from_xml/;

can_ok($module, @methods);

dies_ok {my $ts = $module->new()} "Requesting a service without arguments";

my $ts = new_ok($module => $args_ref, 'Request a service');
can_ok($ts, @methods);

my ($template) = $ts->get_templates;
isa_ok($template, 'InterMine::Template');

dies_ok {my @wanted = $ts->search_for()} 
           'Searching for templates without a keyword';
my ($wanted) = $ts->search_for('probe');

ok(defined $wanted, 'Search for results with a keyword');
isa_ok($wanted, 'InterMine::Template');

