package InterMine::Constraint::Binary;

use Moose;

extends 'InterMine::Constraint';
with 'InterMine::Constraint::Roles::Operator';
use InterMine::TypeLibrary qw(BinaryOperator);
use MooseX::Types::Moose qw(Str);

has '+op' => (
    isa => BinaryOperator,
);

has 'value' => (
    is	     => 'ro',
    isa	     => Str,
    required => 1,
    writer   => 'set_value',
);

override to_string => sub {
    my $self = shift;
    return join(' ', super(), $self->op, '"'.$self->value.'"');
};

override to_hash => sub {
    my $self = shift;
    return (super, $self->operator_hash_bits, value => $self->value);
};

__PACKAGE__->meta->make_immutable;
no Moose;
1;
