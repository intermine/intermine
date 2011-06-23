package Webservice::InterMine::Query::TemplateHandler;

use Moose;
extends 'Webservice::InterMine::Query::Handler';
use Webservice::InterMine::Types qw(Template);

has '+query' => ( isa => Template, );

override start_element => sub {
    my $self = shift;
    my $args = shift;

    # The extra elements for a template are the head (template)
    if ( $args->{Name} eq 'template' ) {
        $self->query->name( $args->{Attributes}{name} ) 
            or confess "No name attribute on template node: ", 
                join(', ', map {"$_ => " . $args->{Attributes}{$_}}
                    keys %{ $args->{Attributes} });
        $self->query->title( $args->{Attributes}{title} )
          if $args->{Attributes}{title};
        if ( $args->{Attributes}{description} ) {
            confess "Template has both description and title attributes"
              if $self->{query}{title};
            $self->query->title( $args->{Attributes}{description} );
        }
        $self->query->description(
            $args->{Attributes}{longDescription} )
          if $args->{Attributes}{longDescription};
        $self->query->comment( $args->{Attributes}{comment} )
          if $args->{Attributes}{comment};
    } else {
        super;
    }
};

# and extra values in the constraint element
override process_constraint_attr => sub {
    my $self = shift;
    my $attr = $self->current_constraint_attr;
    my %temp_args;
    $temp_args{description} = $attr->{description}
      if $attr->{description};
    $temp_args{identifier} = $attr->{identifier}
      if $attr->{identifier};
    if ( $attr->{editable} and $attr->{editable} eq 'true' ) {
        $temp_args{is_editable} = 1;
    } else {
        $temp_args{is_editable} = 0;
    }
    if ( $attr->{switchable} ) {
        $temp_args{is_locked} =
          ( $attr->{switchable} eq 'locked' ) ? 1 : 0;
        $temp_args{switched_on} =
          ( $attr->{switchable} eq 'off' ) ? 0 : 1;
    }
    return ( super, %temp_args );
};

__PACKAGE__->meta->make_immutable;
no Moose;

1;
