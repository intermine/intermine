package Webservice::InterMine::Constraint::Ternary;

use Moose;
extends 'Webservice::InterMine::Constraint::Binary';

use Webservice::InterMine::Types qw(TernaryOperator);
use MooseX::Types::Moose qw(Str);

has '+op' => ( isa => TernaryOperator, coerce => 1);

has 'extra_value' => (
    is  => 'ro',
    isa => Str,
);

override to_string => sub {
    my $self = shift;
    my $ev = (defined $self->extra_value) ? $self->extra_value : 'NULL';
    return join( ' ', super(), 'IN', qq{"$ev"} );
};

override to_hash => sub {
    my $self = shift;
    if ( $self->extra_value ) {    # extraValues are an optional part
        return super, ( extraValue => $self->extra_value );
    } else {
        return super;
    }
};

__PACKAGE__->meta->make_immutable;
no Moose;

1;
