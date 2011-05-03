#!/usr/bin/perl

use strict;
use warnings;

use lib 'lib';
use Benchmark qw(:all);
use Webservice::InterMine;
use Webservice::InterMine::Service;
use Webservice::InterMine::Simple;

my $url = "http://localhost:8080/intermine-test/service";
my @load_args = (source_file => "t/data/employee_summary.xml");
my $as = "string";
my $service = Webservice::InterMine->get_service($url);
my $simple_service = Webservice::InterMine::Simple->get_service($url);

open (my $black_hole, '>', "/dev/null");

my $benchmarks = timethese(300, {
	"6_simple_script" => sub {
		system("perl scripts/simple-service.pl http://localhost:8080/intermine-test/service t/data/employee_summary.xml > /dev/null");
	},
    "6_simple_service_and_query" => sub {
        my $simple_service_inner = Webservice::InterMine::Simple->get_service($url);
        my $query = $simple_service_inner->new_query(model => "testmodel");

        $query->add_view(qw/Employee.name Employee.age/);
        $query->add_constraint(path => "Employee.age", op => '>', value => 35);
        $query->add_constraint(path => "Employee.age", op => '!=', value => 55);

        print $black_hole $query->results(as => "string");
    },
    "5_simple_query" => sub {
        my $query = $simple_service->new_query(model => "testmodel");

        $query->add_view(qw/Employee.name Employee.age/);
        $query->add_constraint(path => "Employee.age", op => '>', value => 35);
        $query->add_constraint(path => "Employee.age", op => '!=', value => 55);

        print $black_hole $query->results(as => "string");
    },
    "3_moose_script" => sub {
        system("run-im-query --url $url t/data/employee_summary.xml > /dev/null");
    },
	"4_lwp_script" => sub {
		system("perl scripts/simple-query-runner.pl http://localhost:8080/intermine-test/service t/data/employee_summary.xml > /dev/null");
	},
	"1_moose_query" => sub {
		my $query = $service->new_from_xml(@load_args);
		print $black_hole $query->results(as => $as, start => 0, size => undef);
	},
	"2_moose_service_and_query" => sub {
		my $inner_service = Webservice::InterMine::Service->new($url);
		my $query = $inner_service->new_from_xml(@load_args);
		print $black_hole $query->results(as => $as, start => 0, size => undef);
	},
});

close $black_hole;

cmpthese($benchmarks);

exit;

__END__

=head1 RESULTS

Benchmark: timing 300 iterations of 1_moose_query, 2_moose_service_and_query, 3_moose_script, 4_lwp_script, 5_simple_query, 6_simple_script, 6_simple_service_and_query...

1_moose_query:  6 wallclock secs ( 4.13 usr +  0.15 sys =  4.28 CPU) @ 70.09/s (n=300)
2_moose_service_and_query: 99 wallclock secs (94.46 usr +  1.90 sys = 96.36 CPU) @  3.11/s (n=300)
3_moose_script: 905 wallclock secs ( 0.02 usr  2.01 sys + 859.88 cusr 33.76 csys = 895.67 CPU) @  0.33/s (n=300)
4_lwp_script: 87 wallclock secs ( 0.02 usr  1.97 sys + 70.75 cusr 10.46 csys = 83.20 CPU) @  3.61/s (n=300)
5_simple_query:  3 wallclock secs ( 1.19 usr +  0.10 sys =  1.29 CPU) @ 232.56/s (n=300)
6_simple_script: 88 wallclock secs ( 0.02 usr  2.69 sys + 71.16 cusr 10.65 csys = 84.52 CPU) @  3.55/s (n=300)
6_simple_service_and_query:  4 wallclock secs ( 1.19 usr +  0.09 sys =  1.28 CPU) @ 234.38/s (n=300)

                              Rate 3_moose_script 2_moose_service_and_query 6_simple_script 4_lwp_script 1_moose_query 5_simple_query 6_simple_service_and_query
3_moose_script             0.335/s             --                      -89%            -91%         -91%         -100%          -100%                      -100%
2_moose_service_and_query   3.11/s           830%                        --            -12%         -14%          -96%           -99%                       -99%
6_simple_script             3.55/s           960%                       14%              --          -2%          -95%           -98%                       -98%
4_lwp_script                3.61/s           977%                       16%              2%           --          -95%           -98%                       -98%
1_moose_query               70.1/s         20827%                     2151%           1875%        1844%            --           -70%                       -70%
5_simple_query               233/s         69332%                     7370%           6452%        6350%          232%             --                        -1%
6_simple_service_and_query   234/s         69874%                     7428%           6503%        6400%          234%             1%                         --
