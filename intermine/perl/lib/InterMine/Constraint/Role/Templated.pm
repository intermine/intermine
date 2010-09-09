package InterMine::Constraint::Role::Templated;

use Moose::Role;
use MooseX::Types::Moose qw(Bool Str);

has editable => (
    is       => 'ro',
    isa      => Bool,
    required => 1,
);

has switchable => (
    is       => 'ro',
    isa      => Bool,
    default  => 0,
);

has switched_on => (
    is       => 'ro',
    isa      => Bool,
    default  => 1,
);

has identifier => (
    is       => 'ro',
    isa      => Str,
    );

override to_hash => sub {
    my $self = shift;
    my %extra;
    $extra{editable} = ($self->editable) ? 'true' : 'false' ;
    if ($self->switchable) {
	$extra{switchable} = ($self->switched_on) ? 'on' : 'off' ;
    } else {
	$extra{switchable} = 'locked';
    }
    $extra{identifer} = $self->identifier if $self->identifier;
    return (super, %extra);
};
1;
