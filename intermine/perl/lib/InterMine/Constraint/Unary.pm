package InterMine::Constraint::Unary;

use Moose;

extends 'InterMine::Constraint';
with 'InterMine::Constraint::Roles::Operator';
use InterMine::TypeLibrary qw(UnaryOperator);

has '+op' => (
    isa => UnaryOperator,
);


override to_string => sub {
    my $self = shift;
    return join(' ', super(), $self->op);
};

override to_hash => sub {
    my $self = shift;
    return (super, $self->operator_hash_bits);
};


__PACKAGE__->meta->make_immutable;

1;
