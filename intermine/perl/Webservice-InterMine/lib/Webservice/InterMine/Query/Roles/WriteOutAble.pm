package Webservice::InterMine::Query::Roles::WriteOutAble;

use MooseX::Role::WithOverloading;
requires(
    qw/name view sort_order logic joins
      path_descriptions model_name
      constraints coded_constraints/
);

use XML::DOM;

use overload (
    '""' => 'stringify',
    fallback => 1,
);

sub stringify {
    my $self = shift;
    if (my $string = eval { $self->to_xml} ) {
        return $string;
    } else {
        return "<query>Invalid</query>";
    }
}

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

has has_been_written_out => (
    traits => ['Bool'],
    is => 'rw',
    default => 0,
    handles => {
        mark_written => 'set',
        mark_dirty   => 'unset',
        needs_writing => 'not',
    },
);

has written_form => (
    isa => 'Str',
    init_arg => undef,
    writer => 'cache_xml',
    reader => 'retrieve_xml',
);

after [qw(
    add_constraint add_join add_view add_outer_join add_sort_order add_pathdescription
    set_logic set_sort_order remove clear_path_descriptions clear_logic clear_joins
    clear_constraints clear_view clear_sort_order
        )] => sub {
    my $self = shift;
    $self->mark_dirty;
};

sub to_xml {
    my $self = shift;
    if ($self->needs_writing) {
        $self->cache_xml($self->to_DOM->toString);
        $self->mark_written;
    }
    return $self->retrieve_xml;
}

sub to_query_xml {
    my $self = shift;
    my $dom = $self->to_DOM;
    my ($query) = ($dom->getTagName eq "query") 
            ? $dom 
            : $dom->getElementsByTagName("query");
    die "no query element found in DOM"
        unless $query;
    return $query->toString;
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
