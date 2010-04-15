#!/usr/bin/perl

use strict;
use warnings;
use LWP::Simple;
use URI;
use Getopt::Long;

my $antibody_target         = 'BEAF-32';
my $developmental_stage     = 'late embryonic stage';
my $cellline_name           = 'S2-DRSC';
my $no_of_results_to_return = 100;

my $result = GetOptions(
    "antibody_target=s"              => \$antibody_target,
    "developmental_stage=s"          => \$developmental_stage,
    "cellline_name=s"                => \$cellline_name,
    "size|no_of_results_to_return=i" => \$no_of_results_to_return,
    );


my $url = 'http://intermine.modencode.org/query/service/template/results?name=AntibodyTargetGeneDevStageCellLine_BindingSites&constraint1=Submission.developmentalStages.name&op1=eq&value1=SEARCHTERM1&constraint2=Submission.antibodies.target&op2=LOOKUP&value2=SEARCHTERM0&extra2=D.+melanogaster&constraint3=Submission.cellLines.name&op3=eq&value3=SEARCHTERM2&size=SEARCHTERM3&format=tab';

my $i = 0;
for ($antibody_target, $developmental_stage, $cellline_name, $no_of_results_to_return) {
    my $uri_fragment = URI->new($_);          # This escapes the replacements to use in a url
    $url =~ s/SEARCHTERM$i/$uri_fragment/g;
    $i++;
}

my $res = getprint($url);                           # This gets the document, and prints it out.

