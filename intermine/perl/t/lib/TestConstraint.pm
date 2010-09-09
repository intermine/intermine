package TestConstraint;

use base qw(TestPathFeature);
use Test::More;

my $module = 'InterMine::Constraint';

sub inheritance : Test(2) {
    ok($module->isa('InterMine::PathFeature');
    ok($module->does('InterMine::Roles::Logical');
}

sub requirements : Test(3) {
    ok($module->requirements_are_met_by(%args),
       "$module declares itself happy with the arg list");
    ok($module->requirements_are_met_by(%args, foo => 'bar'),
       "$module declares itself happy even when there is something extra");
    ok(! $module->requirements_are_met_by(),
       "$module declares itself unhappy with the insufficient arg list");
}

1;
