#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 22;
use Test::Exception;

use InterMine::PathQuery::Constraint;

dies_ok {InterMine::PathQuery::Constraint->new('FOO')} 'illegal op';
dies_ok {InterMine::PathQuery::Constraint->new('IS NULL foo')} 'unary op with value';
dies_ok {InterMine::PathQuery::Constraint->new('CONTAINS')} 'binary op with no value';

my $not_null_c = InterMine::PathQuery::Constraint->new('IS NOT NULL');
is($not_null_c->op(), 'IS NOT NULL', 'IS NOT NULL');
ok(!defined $not_null_c->value());

my $null_c = InterMine::PathQuery::Constraint->new('IS NULL');
is($null_c->op(), 'IS NULL', 'IS NULL');
ok(!defined $null_c->value());

my $equals_c = InterMine::PathQuery::Constraint->new('= "zen"');
is($equals_c->op(), '=', '=');
ok(defined $equals_c->value());
is($equals_c->value(), "zen", 'value is "zen"');

my $not_equals_c = InterMine::PathQuery::Constraint->new('!= "zen"');
is($not_equals_c->op(), '!=', '!=');
ok(defined $not_equals_c->value());
is($not_equals_c->value(), "zen", 'value is "zen"');

my $lt_c = InterMine::PathQuery::Constraint->new('< 100');
is($lt_c->op(), '<', '<');
ok(defined $lt_c->value());
is($lt_c->value(), 100, 'value is 100');

my $gt_c = InterMine::PathQuery::Constraint->new('> 100');
is($gt_c->op(), '>', '>');
ok(defined $gt_c->value());
is($gt_c->value(), 100, 'value is 100');

my $contains_c = InterMine::PathQuery::Constraint->new('CONTAINS "enzyme"');
is($contains_c->op(), 'CONTAINS', 'CONTAINS');
ok(defined $contains_c->value());
is($contains_c->value(), "enzyme", 'value is "enzyme"');
