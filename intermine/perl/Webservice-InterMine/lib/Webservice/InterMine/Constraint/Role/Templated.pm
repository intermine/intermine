package Webservice::InterMine::Constraint::Role::Templated;

use Moose::Role;
use MooseX::Types::Moose qw(Bool Str);

# TODO: this attribute will govern permissions when authenication
# is implemented - it needs to be set by the Handler when the template
# is read in on the service level
has is_writable => (
    is      => 'ro',
    isa     => Bool,
    default => 0,
);

has is_editable => (
    traits   => ['Bool'],
    is       => 'ro',
    isa      => Bool,
    required => 1,
    handles  => {
        hide   => 'unset',
        unhide => 'set',
    },
);

# Ugliness because roles don't support regexp before meth-mods
before hide   => \&check_writability;
before unhide => \&check_writability;
before lock   => \&check_writability;
before unlock => \&check_writability;

sub check_writability {
    my $self = shift;
    unless ( $self->is_writable ) {
        confess
"You cannot make changes to this template unless you are authenticated as its owner";
    }
}

has is_locked => (
    traits  => ['Bool'],
    is      => 'ro',
    isa     => Bool,
    default => 1,
    trigger => sub {
        my $self  = shift;
        my $value = shift;
        if ( not $value ) {
            confess "Only editable constraints can be switchable"
              unless $self->is_editable;
        }
    },
    handles => {
        lock   => 'set',
        unlock => 'unset',
    },
);

has switched_on => (
    traits  => ['Bool'],
    is      => 'ro',
    isa     => Bool,
    default => 1,
    trigger => sub {
        my $self  = shift;
        my $value = shift;
        if ( not $value ) {
            confess "Only switchable constraints can be switched off"
              if $self->is_locked;
        }
    },
    handles => {
        switch_on  => 'set',
        switch_off => 'unset',
    }
);

has [qw/identifier description/] => (
    is  => 'ro',
    isa => Str,
);

sub query_hash {
    my $self = shift;
    my %hash = $self->to_hash('query');
    $hash{constraint} = $hash{path};
    delete $hash{path};
    return %hash;
}

sub switchable_string {
    my $self = shift;
    if ( $self->is_locked ) {
        return 'locked';
    } else {
        return ( $self->switched_on ) ? 'on' : 'off';
    }
}

override to_hash => sub {
    my $self = shift;
    return super if ( $_[0] and $_[0] eq 'query' );
    my %extra;
    $extra{editable}    = ( $self->is_editable ) ? 'true' : 'false';
    $extra{switchable}  = $self->switchable_string;
    $extra{identifier}  = $self->identifier;
    $extra{description} = $self->description;
    return ( super, %extra );
};

override to_string => sub {
    my $self = shift;
    return super . ' (' . $self->switchable_string . ')';
};

1;
