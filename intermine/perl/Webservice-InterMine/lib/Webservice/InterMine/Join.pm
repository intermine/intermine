package Webservice::InterMine::Join;

use Moose;
extends 'Webservice::InterMine::PathFeature';

use Webservice::InterMine::Types qw(JoinStyle);

around BUILDARGS => sub {
    my $orig  = shift;
    my $class = shift;

    if ( @_ >= 2 and $_[0] eq 'path' ) {
        return $class->$orig(@_);
    } else {
        my %args = ( path => shift );
        $args{style} = shift if @_;
        return $class->$orig(%args);
    }
};

has style => (
    is       => 'ro',
    isa      => JoinStyle,
    required => 1,
    default  => 'OUTER'
);

override to_string => sub {
    my $self = shift;
    return super . ' is an ' . $self->style . ' join';
};

override to_hash => sub {
    my $self = shift;
    return ( super, style => $self->style );
};

sub _build_element_name {
    return 'join';
}

__PACKAGE__->meta->make_immutable;
no Moose;

1;
