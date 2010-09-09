use strict;
use warnings;
use Carp;

use Test::More tests => 10;
use Test::Exception;

use InterMine::ConstraintFactory;

my $module = 'InterMine::LogicalSet';
my $role   = 'InterMine::Roles::Logical';

my $factory = InterMine::ConstraintFactory->new;
my $unary_con = $factory->make_constraint(
    path => 'Some.path',
    op   => 'IS NULL',
);
my $binary_con =  $factory->make_constraint(
    path => 'Some.other.path',
    op   => '=',
    value => 500,
);
my $ternary_con = $factory->make_constraint(
    path => 'Yet.another.path',
    op   => 'LOOKUP',
    value => 500,
    extra_value => 'Foo',
);
my $multi_constraint = $factory->make_constraint(
    path => 'And.yet.another.path',
    op   => 'IN',
    values => [qw/one two three/],
);
my $subclass_constraint = $factory->make_constraint(
    path => 'Other.path',
    type => 'Alleyway',
);

my $and_logic = ($unary_con & $binary_con & $ternary_con & $multi_constraint);

ok($and_logic->does($role), "Produces a good AND logic set");
is($and_logic->code, 'A and B and C and D', "... and it stringifies correctly");
is("$and_logic", 'A and B and C and D', "... also when overloaded");

my $or_logic = ($unary_con | $binary_con | $ternary_con | $multi_constraint);

ok($or_logic->does($role), "Produces a good OR logic set");
is($or_logic->code, '(((A or B) or C) or D)', "... and it stringifies correctly");
is("$or_logic", '(((A or B) or C) or D)', "... also when overloaded");

my $mixed_logic = (($unary_con | $binary_con) & ($ternary_con | $multi_constraint));

ok($mixed_logic->does($role), "Produces a good MIXED logic set");
is($mixed_logic->code, '(A or B) and (C or D)', "... and it stringifies correctly");
is("$mixed_logic", '(A or B) and (C or D)', "... also when overloaded");

dies_ok(
    sub {
	my $bad_logic = ($unary_con & $binary_con & $ternary_con & $multi_constraint & $subclass_constraint);
    },
    "Dies when putting non-logical items into the mix",
);

