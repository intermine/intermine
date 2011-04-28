package Webservice::InterMine::Constraint::Multi;

use Moose;

extends 'Webservice::InterMine::Constraint';
with 'Webservice::InterMine::Constraint::Role::Operator';

use Webservice::InterMine::Types qw(MultiOperator);
use MooseX::Types::Moose qw(ArrayRef Str);

has '+op' => ( isa => MultiOperator, coerce => 1 );

has 'values' => (
    is       => 'ro',
    isa      => ArrayRef [Str],
    required => 1,
);

override to_string => sub {
    my $self = shift;
    return join( ' ',
        super(), $self->op,
        '"' . join( ', ', @{ $self->values } ) . '"' );
};

override to_hash => sub {
    my $self = shift;
    return super, $self->operator_hash_bits, ( value => $self->values );
};

__PACKAGE__->meta->make_immutable;
no Moose;
1;
