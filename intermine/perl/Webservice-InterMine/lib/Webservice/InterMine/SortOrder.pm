package Webservice::InterMine::SortOrder;

use Moose;
extends 'Webservice::InterMine::PathFeature';

use Webservice::InterMine::Types qw(SortDirection);

around BUILDARGS => sub {
    my $orig  = shift;
    my $class = shift;
    if ( @_ <= 2 and $_[0] and $_[0] ne 'path' ) {
        my @args;
        if ( ref $_[0] eq 'ARRAY' ) {
            @args = @$_;
        } else {
            @args = @_;
        }
        my %args;
        $args{path} = shift @args;
        $args{direction} = shift @args if @args;
        return $class->$orig(%args);
    } else {
        return $class->$orig(@_);
    }
};

use overload (
    '""'     => 'to_string',
    fallback => 1,
);

has direction => (
    is      => 'ro',
    isa     => SortDirection,
    coerce  => 1,
    lazy    => 1,
    default => 'asc',
);

override 'to_string' => sub {
    my $self = shift;
    return join( ' ', super, $self->direction );
};

__PACKAGE__->meta->make_immutable;
no Moose;
1;
