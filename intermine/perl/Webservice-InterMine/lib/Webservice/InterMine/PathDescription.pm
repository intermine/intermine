package Webservice::InterMine::PathDescription;

use Moose;
extends 'Webservice::InterMine::PathFeature';
with 'Webservice::InterMine::Role::Described';

has '+description' => ( required => 1 );

around BUILDARGS => sub {
    my ( $orig, $class, @args ) = @_;
    if ( @args != 2 ) {
        return $class->$orig(@args);
    } else {
        my %args;
        @args{qw/path description/} = @args;
        return $class->$orig(%args);
    }
};

before description => sub {
    my $self = shift;
    confess "Cannot change description - it is read-only" if @_;
};

override to_hash => sub {
    my $self = shift;
    return ( pathString => $self->path,
        description => $self->description );
};

override to_string => sub {
    my $self = shift;
    return super . q{: "} . $self->description . q{"};
};

sub _build_element_name {
    return 'pathDescription';
}

__PACKAGE__->meta->make_immutable;
no Moose;

1;
