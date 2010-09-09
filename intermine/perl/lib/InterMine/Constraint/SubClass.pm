package InterMine::Constraint::SubClass;

use Moose;

extends 'InterMine::Constraint';

has 'type' => (
    is  => 'ro',
    isa => 'Str',
    required => 1,
    );

override to_string => sub {
    my $self = shift;
    return super() . ' is a ' . $self->type;
};

override to_hash => sub {
    my $self = shift;
    return super, (type => $self->type);
};

__PACKAGE__->meta->make_immutable;

1;
