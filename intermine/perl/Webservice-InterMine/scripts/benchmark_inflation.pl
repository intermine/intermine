#!/usr/bin/perl

use strict;
use warnings;

use lib 'lib';

use Webservice::InterMine;
use InterMine::Model;
use Webservice::InterMine::Query::Roles::Runnable;
use JSON -support_by_pp, -no_export;

use Benchmark qw(:all);
use Data::Dumper;
use Devel::Size 'total_size';

my $json = JSON->new->allow_singlequote->allow_barekey;
my $model = InterMine::Model->new(file => "t/data/testmodel_model.xml");

open( my $fh, '<', 't/data/result.json' );
my $results = do {local $/; <$fh>};

my $to_count = $results;

my $count = (($to_count =~ s/("class":)//g) + 1);

print "$count objects\n";

my $structures = {};

my $benchmarks = timethese(10, {
    "Inflation" => sub {
        my $json_results = $json->decode($results);
        Webservice::InterMine::Query::Roles::Runnable::inflate($json_results->{results});
        $structures->{"Inflation"} = $json_results->{results};
    },
    "Instantiation" => sub {
        my $json_results = $json->decode($results);
        $structures->{"Instantiation"} = 
            [ map {$model->make_new($_)} @{ $json_results->{results} } ];
    },
});

print "\n";

for my $run (sort {$benchmarks->{$a}[0] <=> $benchmarks->{$b}[0]} keys %$benchmarks) {
    my $data = $benchmarks->{$run};
    my $size; $size += total_size($_) for @{ $structures->{$run} };
    printf "%s:\t%5.2f objects per second, %5d bytes (%5d bytes per object)\n", 
        $run, 
        ($data->iters * $count)/$data->cpu_a, 
        $size, $size/$count;
}

open(my $out, '>', '/tmp/instantiated.dump');
print $out Dumper($structures->{Instantiation});
close($out);

=head2 Benchmark inflation versus instantiation

Prints out results like:

    1774 objects
    Benchmark: timing 10 iterations of Inflation, Instantiation...
    Inflation:  9 wallclock secs ( 9.60 usr +  0.00 sys =  9.60 CPU) @  1.04/s (n=10)
    Instantiation: 15 wallclock secs (14.91 usr +  0.01 sys = 14.92 CPU) @  0.67/s (n=10)

    Inflation:      1847.92 objects per second, 489786 bytes (  276 bytes per object)
    Instantiation:  1189.01 objects per second, 110970 bytes (   62 bytes per object)
