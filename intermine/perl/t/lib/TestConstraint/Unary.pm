package TestConstraint::Unary;

use base qw(TestConstraint);
use Test::More;

my $module = 'InterMine::Constraint::Unary';
my $args   = (
    path => 'Some.unary.path',
    op   => 'IS NULL',
);

sub inheritance : Test {
    my $self = shift;
    ok($module->isa('InterMine::Constraint'));
    $self->SUPER::inheritance;
}

sub strict_construction : Test(2) {
    my $self = shift;
    my @valid_operators = ('IS NOT NULL', 'IS NULL');
    for (@valid_operators) {
	new_ok($module => [path => 'Some.path', op => $_]);
    }
    $self->SUPER::strict_construction;
}

sub methods {
    my $self = shift;
    can_ok($module, (qw/op code set_code/));
    $self->SUPER::methods;
}

1;
