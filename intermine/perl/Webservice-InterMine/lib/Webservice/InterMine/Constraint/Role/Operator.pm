package Webservice::InterMine::Constraint::Role::Operator;

use Moose::Role;
with 'Webservice::InterMine::Role::Logical';

use Carp;
use MooseX::Types::Moose qw(Str);
use InterMine::TypeLibrary qw(ConstraintCode);

my $next_code = 'A';
my @used_codes;

has 'op' => (
    is       => 'ro',
    isa      => Str,
    required => 1,
);

has 'code' => (
    is          => 'ro',
    writer      => 'set_code',
    isa         => ConstraintCode,
    default     => sub { $next_code++ },
    initializer => 'set_code',
);

sub operator_hash_bits {
    my $self = shift;
    return ( op => $self->op, code => $self->code );
}

1;
