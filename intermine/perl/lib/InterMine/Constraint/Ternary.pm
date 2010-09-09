package InterMine::Constraint::Ternary;

use Moose;
extends 'InterMine::Constraint::Binary';

use InterMine::TypeLibrary qw(TernaryOperator);
use MooseX::Types::Moose qw(Str);

has '+op' => (
    isa   => TernaryOperator,
);

has 'extra_value' => (
    is	     => 'ro',
    isa	     => Str,
);

override to_string => sub {
    my $self = shift;
    return join(' ', super(), 'IN "'.$self->extra_value.'"');
};

override to_hash => sub {
    my $self = shift;
    if ($self->extra_value) { # extraValues are an optional part
	return super, (extraValue => $self->extra_value);
    } else {
	return super;
    }
};

__PACKAGE__->meta->make_immutable;
no Moose;

1;
