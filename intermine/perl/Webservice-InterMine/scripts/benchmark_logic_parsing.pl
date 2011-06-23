#!/usr/bin/perl

use lib 'lib';
use Benchmark qw(:all);

use Carp;

$SIG{__WARN__} = \&Carp::cluck;
$SIG{__DIE__} = \&Carp::confess;

use Webservice::InterMine "squirrel.flymine.org/intermine-test";

my $q = Webservice::InterMine->new_query;
my $p = Webservice::InterMine->new_query;

my %cons;
for my $code ('A' .. 'Z') {
    for my $query ($q, $p) {
        $cons{$code} = $query->add_constraint(
            path => "Employee.name",
            op => '=',
            value => 'Phil',
            code => $code,
        );
    }
}

my $logic_string = "A or B and C or D and E or F or G and H or I and J or K and L or M and N or O and P or Q and R and S or T and U and V and W or X and Y and Z";

print $q->logic_parser->parse_logic($logic_string), "\n";
print $p->logic($cons{A} | $cons{B} & $cons{C} | $cons{D} & $cons{E} | $cons{F} | $cons{G} & $cons{H} | $cons{I} & $cons{J} | $cons{K} & $cons{L} | $cons{M} & $cons{N} | $cons{O} & $cons{P} | $cons{Q} & $cons{R} & $cons{S} | $cons{T} & $cons{U} & $cons{V} & $cons{W} | $cons{X} & $cons{Y} & $cons{Z}), "\n";
print $q->logic($logic_string), "\n";

my $results =timethese(100_000 => {
    full_parsing => sub {$q->logic_parser->parse_logic($logic_string)},
    no_evaluation => sub {$q->logic_parser->parse_logic_without_evaluation($logic_string)},
});

cmpthese( $results );

$results = timethese(100_000 => {
  "string eval parsing" => sub {$q->check_logic($logic_string)},
  "coercion parsing"    => sub {$q->logic($cons{A} | $cons{B} & $cons{C} | $cons{D} & $cons{E} | $cons{F} | $cons{G} & $cons{H} | $cons{I} & $cons{J} | $cons{K} & $cons{L} | $cons{M} & $cons{N} | $cons{O} & $cons{P} | $cons{Q} & $cons{R} & $cons{S} | $cons{T} & $cons{U} & $cons{V} & $cons{W} | $cons{X} & $cons{Y} & $cons{Z})},
  "stream parsing"      => sub {$q->logic($q->logic_parser->parse_logic($logic_string))},
  });

cmpthese( $results );




    
