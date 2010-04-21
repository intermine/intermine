#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 29;
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

my $LOOKUP_c =  InterMine::PathQuery::Constraint->new('LOOKUP "foo"');
is($LOOKUP_c->op(), 'LOOKUP', 'LOOKUP');
ok(defined $LOOKUP_c->value());
is($LOOKUP_c->value(), 'foo', q/value is 'foo'/);

my $mutable_c = InterMine::PathQuery::Constraint->new('LOOKUP "foo"');
$mutable_c->value('bar');
is($mutable_c->value, 'bar', "Can change value");
$mutable_c->op('IS NULL');
ok(! defined $mutable_c->value);
is($mutable_c->op, 'IS NULL', "can change operator");

my $extra_value_c = InterMine::PathQuery::Constraint->new('LOOKUP "foo"');
$extra_value_c->extra_value('baz');
is($extra_value_c->extra_value, 'baz', "Can set extra value");
