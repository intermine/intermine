package Webservice::InterMine::Query::Roles::ExtendedQuery;

use Moose::Role;
use Webservice::InterMine::Types qw(QueryType);
use MooseX::Types::Moose qw(HashRef);

requires(qw/type to_DOM insertion head apply_attributes_to_element/);

around to_DOM => sub {
    my $orig  = shift;
    my $self  = shift;
    my $query = $self->$orig;
    my $doc   = $query->getOwnerDocument;
    my $head  = $doc->createElement( $self->type );
    $self->apply_attributes_to_element( $head, %{ $self->head } );
    $head->appendChild($query);

    my $insertions = $self->insertion;

    while ( my ( $tag, $hash ) = each %$insertions ) {
        my $elem = $doc->createElement($tag);
        $self->apply_attributes_to_element( $elem, %$hash );
        $query->appendChild($elem);
    }

    return $head;
};

1;
