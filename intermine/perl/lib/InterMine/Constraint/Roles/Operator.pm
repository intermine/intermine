package InterMine::Constraint::Roles::Operator;

use Moose::Role;
with 'InterMine::Roles::Logical';

use Carp;
use MooseX::Types::Moose qw(Str);
use InterMine::TypeLibrary qw(ConstraintCode);

my $next_code = 'A';
my @used_codes;

has 'op' => (
    is  => 'ro',
    isa => Str,
    required => 1,
);

has 'code' => (
    is  => 'ro',
    writer => 'set_code',
    isa => ConstraintCode,
    default  => sub {$next_code++},
    initializer => 'set_code',
);

sub operator_hash_bits {
    my $self = shift;
    return (op => $self->op, code => $self->code);
}

1;
