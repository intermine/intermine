package Webservice::InterMine::Query::SavedHandler;

use Moose;
extends 'Webservice::InterMine::Query::Handler';
use Webservice::InterMine::Types qw(SavedQuery);

has '+query' => ( isa => SavedQuery, );

override start_element => sub {
    my $self = shift;
    my $args = shift;

    # The extra element for a saved-query is the head
    if ( $args->{Name} eq 'saved-query' ) {
        $self->query->name( $args->{Attributes}{name} );
        $self->query->date( $args->{Attributes}{'date-created'} )
          if $args->{Attributes}{'date-created'};
    } else {
        super;
    }
};

__PACKAGE__->meta->make_immutable;
no Moose;

1;
