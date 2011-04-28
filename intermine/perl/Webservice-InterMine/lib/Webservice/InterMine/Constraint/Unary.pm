package Webservice::InterMine::Constraint::Unary;

use Moose;

extends 'Webservice::InterMine::Constraint';
with 'Webservice::InterMine::Constraint::Role::Operator';
use Webservice::InterMine::Types qw(UnaryOperator);

has '+op' => ( isa => UnaryOperator, coerce => 1);

override to_string => sub {
    my $self = shift;
    return join( ' ', super(), $self->op );
};

override to_hash => sub {
    my $self = shift;
    return ( super, $self->operator_hash_bits );
};

__PACKAGE__->meta->make_immutable;
no Moose;

1;
