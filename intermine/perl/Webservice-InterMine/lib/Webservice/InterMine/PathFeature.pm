package Webservice::InterMine::PathFeature;

use Moose;

#use InterMine::Model; # Can't quite work out why, but it doesn't compile without this
use InterMine::Model::Types qw(PathString);

around BUILDARGS => sub {
    my $orig  = shift;
    my $class = shift;
    my %args  = @_;
    my @good_attributes =
      map { $_->name } $class->meta->get_all_attributes;
    for my $arg ( keys %args ) {
        confess
          "Invalid parameter $arg => $args{$arg} passed to $class->new"
          unless ( grep { $arg eq $_ } @good_attributes );
    }
    return $class->$orig(%args);
};

has path => (
    is       => 'ro',
    writer   => 'set_path',
    isa      => PathString,
    coerce   => 1,
    required => 1,
);

has element_name => (
    is         => 'ro',
    lazy_build => 1,
    init_arg   => undef,
);

sub _build_element_name {
    confess "You are not meant to have instantiated a PathFeature";
}

sub to_string {
    my $self = shift;
    return $self->path;
}

sub to_hash {
    my $self = shift;
    return ( path => $self->path );
}
__PACKAGE__->meta->make_immutable;
no Moose;
1;
