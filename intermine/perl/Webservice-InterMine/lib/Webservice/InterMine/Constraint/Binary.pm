package Webservice::InterMine::Constraint::Binary;

use Moose;

extends 'Webservice::InterMine::Constraint';
with 'Webservice::InterMine::Constraint::Role::Operator';
use InterMine::TypeLibrary qw(BinaryOperator);
use MooseX::Types::Moose qw(Str);

has '+op' => ( isa => BinaryOperator, );

has 'value' => (
    is       => 'ro',
    isa      => Str,
    required => 1,
    writer   => 'set_value',
);

has 'extra_value' => (
    is       => 'ro',
    isa      => Str,
);

override to_string => sub {
    my $self = shift;
    return join( ' ', super(), $self->op, '"' . $self->value . '"' );
};

override to_hash => sub {
    my $self = shift;
    return ( super, $self->operator_hash_bits, value => $self->value );
};

__PACKAGE__->meta->make_immutable;
no Moose;
1;
