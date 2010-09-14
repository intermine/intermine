package Webservice::InterMine::Query::Roles::WriteOutAble;

use Moose::Role;
requires(
    qw/name view sort_order logic joins
      path_descriptions model_name
      constraints coded_constraints/
);

use XML::DOM;

sub query_attributes {
    my $self  = shift;
    my %query = (
        name      => $self->name,
        view      => $self->joined_view(' '),
        sortOrder => $self->sort_order,
        model     => $self->model_name,
    );
    $query{longDescription} = $self->description if $self->description;
    $query{constraintLogic} = $self->logic->code
      if ( $self->coded_constraints > 1 );
    return %query;
}

sub apply_attributes_to_element {
    my $self    = shift;
    my $element = shift;
    my $doc = $element->getOwnerDocument;
    my %attrs   = @_;
    while ( my ( $tag, $value ) = each %attrs ) {
	if (ref $value eq 'ARRAY') {
	    for (@$value) {
		my $sub_elem = $doc->createElement($tag);
		my $text = $doc->createTextNode($_);
		$sub_elem->appendChild($text);
		$element->appendChild($sub_elem);
	    }
	} else {
	    $element->setAttribute( $tag => $value );
	}
    }
}

sub to_xml {
    my $self = shift;
    return $self->to_DOM->toString;
}

sub to_DOM {
    my $self = shift;

    my $doc   = new XML::DOM::Document;
    my $query = $doc->createElement('query');

    $self->apply_attributes_to_element( $query,
        $self->query_attributes );

    my @elements = qw(
      path_descriptions
      joins
      all_constraints
    );

    for my $meth (@elements) {
        for my $e ( $self->$meth ) {
            my $elem = $doc->createElement( $e->element_name );
            $self->apply_attributes_to_element( $elem, $e->to_hash );
            $query->appendChild($elem);
        }
    }
    return $query;
}

1;
