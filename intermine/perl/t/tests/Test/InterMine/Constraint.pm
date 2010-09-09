package Test::InterMine::Constraint;

use base ('Test::InterMine::PathFeature');
use Test::More;

sub class {'InterMine::Constraint'}


sub methods : Test(2) {
    my $test = shift;
    $test->SUPER::methods;
    can_ok($test->{object}, (qw/requirements_are_met_by/));
}

sub inheritance : Test(2) {
    my $test = shift;
    my $parent = 'InterMine::PathFeature';
    ok($test->{object}->isa($parent), "Inherits from $parent");

}

sub requirements : Test(3) {
    my $test = shift;
    ok($test->class->requirements_are_met_by($test->args),
       "... declares itself happy with the arg list");
    ok($test->class->requirements_are_met_by($test->args, foo => 'bar'),
       "... declares itself happy even when there is something extra");
    ok(! $test->class->requirements_are_met_by(),
       "... declares itself unhappy with the insufficient arg list");
}

1;
