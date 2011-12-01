#!/usr/bin/perl

use strict;
use warnings;

use lib 'lib';

use Webservice::InterMine;
use Benchmark qw(:all);


my $test_service = get_service("localhost/enormocorp");

my $test_query = $test_service->resultset("Employee")->select(qw/name age fullTime end/)->where("department.company.name" => {ne => "Difficult*"});
my $test_expected_count = 20_000;

printf "%s (%s) %d rows\n", $test_service->root, $test_service->version, $test_expected_count;

cmpthese(10, {
    jsonobjects => sub {
        my $it = $test_query->iterator(as => "ro", size => 20_000);
        my $c = 0;
        while(my $employee = <$it>) {
            $c++;
        }
        die "bad count" . $c unless ($c == $test_expected_count);
    },
    jsonobjects_raw => sub {
        my $it = $test_query->iterator(as => "jsonobjects", size => 20_000);
        my $c = 0;
        while(my $employee = <$it>) {
            $c++;
        }
        die "bad count" . $test_query->count() unless ($c == $test_expected_count);
    },
    jsonrows_raw => sub {
        my $it = $test_query->iterator(as => "jsonrows", size => 20_000);
        my $c = 0;
        while(my $employee = <$it>) {
            $c++;
        }
        die "bad count" . $test_query->count() unless ($c == $test_expected_count);
    },
    jsonrows => sub {
        my $it = $test_query->iterator(as => "rr", size => 20_000);
        my $c = 0;
        while(my $employee = <$it>) {
            $c++;
        }
        die "bad count" unless ($c == $test_expected_count);
    },
    minimal_json_raw => sub {
        my $it = $test_query->iterator(as => "json", size => 20_000);
        my $c = 0;
        while(my $employee = <$it>) {
            $c++;
        }
        die "bad count" unless ($c == $test_expected_count);
    },
    tsv => sub {
        my $it = $test_query->iterator(as => "string", size => 20_000);
        my $c = 0;
        while(my $employee = <$it>) {
            $c++;
        }
        die "bad count: " . $c unless ($c == $test_expected_count);
    },
});

$test_query = $test_service->resultset("Employee")->select(qw/name age fullTime end/)->where(name => "c*");
$test_expected_count = $test_query->count();

printf "%s (%s) %d rows\n", $test_service->root, $test_service->version, $test_expected_count;

cmpthese(40, {
    jsonobjects => sub {
        my $it = $test_query->iterator(as => "ro");
        my $c = 0;
        while(my $employee = <$it>) {
            $c++;
            die "bad employee" unless ($employee->name =~ /^c/i);
        }
        die "bad count" . $test_query->count() unless ($c == $test_expected_count);
    },
    jsonobjects_raw => sub {
        my $it = $test_query->iterator(as => "jsonobjects");
        my $c = 0;
        while(my $employee = <$it>) {
            $c++;
            die "bad employee" unless ($employee->{name} =~ /^c/i);
        }
        die "bad count" . $test_query->count() unless ($c == $test_expected_count);
    },
    jsonrows_raw => sub {
        my $it = $test_query->iterator(as => "jsonrows");
        my $c = 0;
        while(my $employee = <$it>) {
            $c++;
            die "bad employee" unless ($employee->[0]{value} =~ /^c/i);
        }
        die "bad count" . $test_query->count() unless ($c == $test_expected_count);
    },
    jsonrows => sub {
        my $it = $test_query->iterator(as => "rr");
        my $c = 0;
        while(my $employee = <$it>) {
            $c++;
            die "bad employee" unless ($employee->{name} =~ /^c/i);
        }
        die "bad count" unless ($c == $test_expected_count);
    },
    minimal_json_raw => sub {
        my $it = $test_query->iterator(as => "json");
        my $c = 0;
        while(my $employee = <$it>) {
            $c++;
            die "bad employee" unless ($employee->[0] =~ /^c/i);
        }
        die "bad count" unless ($c == $test_expected_count);
    },
    tsv => sub {
        my $it = $test_query->iterator(as => "string");
        my $c = 0;
        while(my $employee = <$it>) {
            $c++;
            die "bad employee" unless ($employee =~ /^c/i);
        }
        die "bad count" unless ($c == $test_expected_count);
    },
});


my $service = get_service("flymine.org/query");
my $query = $service->resultset("Gene")
                    ->select(qw/symbol length/)
                    ->where(symbol => "c*");
my $expected_count = $query->count;

printf "%s (%s) %d rows\n", $service->root, $service->version, $expected_count;

cmpthese(10, {
    jsonobjects => sub {
        my $it = $query->iterator(as => "ro");
        my $c = 0;
        while(my $gene = <$it>) {
            $c++;
            die "bad gene" unless ($gene->symbol =~ /^c/i);
        }
        die "bad count" unless ($c == $expected_count);
    },
    jsonrows => sub {
        my $it = $query->iterator(as => "rr");
        my $c = 0;
        while(my $gene = <$it>) {
            $c++;
            die "bad gene" unless ($gene->{symbol} =~ /^c/i);
        }
        die "bad count" unless ($c == $expected_count);
    },
    tsv => sub {
        my $it = $query->iterator(as => "string");
        my $c = 0;
        while(my $gene = <$it>) {
            $c++;
            die "bad gene" unless ($gene =~ /^c/i);
        }
        die "bad count" unless ($c == $expected_count);
    },
});

