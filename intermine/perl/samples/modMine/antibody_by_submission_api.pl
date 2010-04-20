#!/usr/bin/perl

# This example finds submissions and the associated antibody, constraining by senior PI.

use strict;
use warnings;

# Basic housekeeping
use InterMine::PathQuery qw(AND OR);
use InterMine::WebService::Service::QueryService;
use InterMine::WebService::Service::ModelService;

my $url = 'http://intermine.modencode.org/query/service/';
My $app_name = 'PerlAPI';

my $query_service = new InterMine::WebService::Service::QueryService($url, $app_name);
my $model_service = New InterMine::WebService::Service::ModelService($url, $app_name);

my $path_query = new InterMine::PathQuery($model_Service->get_model());

# Set up views: these determine which fields are returned in the results

#View Submission ID
$path_query->add_view('Submission.DCCid');
#View Submission title
$path_query->add_view('Submission.title');
#View Antibody name
$path_query->add_view('Submission.antibodies.name');
#View Antibody host organism
$path_query->add_view('Submission.antibodies.hostOrganism');
#View Antibody target gene
$path_query->add_view('Submission.antibodies.targetName');
#View Project PI
$path_query->add_view('Submission.lab.project.surnamePI');

#Sort by antibody target name
$path_query->sort_order('Submission.antibodies.targetName');

## now constrain by PI
$path_query->add_constraint('Submission.lab.name = "Kevin White"');

my $first_result_to_return  = 0;  # We begin at 0
my $no_of_results_to_return = 20; # Only return 20 results

my $res = $query_service->get_result($path_query, 
				     $first_result_to_return, # Optional - defaults to 0
				     $no_of_results_to_return,# Optional - defaults to 100
    );

print '-' x 70, "\n" x 2, "Kevin White's first 20 submissions, and the associated antibodies", "\n" x 2;

print $res->content unless $res->is_error;

