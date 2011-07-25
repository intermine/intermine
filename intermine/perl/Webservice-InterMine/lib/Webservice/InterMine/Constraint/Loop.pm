package Webservice::InterMine::Constraint::Loop;

use Moose;

extends 'Webservice::InterMine::Constraint';
with 'Webservice::InterMine::Constraint::Role::Operator';
use Webservice::InterMine::Types qw(LoopOperator);
use MooseX::Types::Moose qw(Str);

has '+op' => ( isa => LoopOperator, coerce => 1);

has 'loop_path' => (
    is       => 'ro',
    isa      => Str,
    required => 1,
    writer   => 'set_loop_path',
    coerce   => 1,
);

override to_string => sub {
    my $self = shift;
    return join( ' ', super(), $self->op, '"' . $self->loop_path . '"' );
};

override to_hash => sub {
    my $self = shift;
    return ( super, $self->operator_hash_bits, loopPath => $self->loop_path );
};

__PACKAGE__->meta->make_immutable;
no Moose;
1;
