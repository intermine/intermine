package Webservice::InterMine::Query::Handler;

use Moose;
use Webservice::InterMine::Types qw(Query);
use MooseX::Types::Moose qw(HashRef ArrayRef Str);

has query => (
    is  => 'rw',
    isa => Query,
);

has element_stack => (
    traits  => ['Array'],
    is      => 'ro',
    isa     => ArrayRef [Str],
    default => sub { [] },
    handles => {
        push_element  => 'push',
        pop_element   => 'pop',
        grep_elements => 'grep',
        open_elements => 'elements',
    },
);

sub within_element {
    my $self      = shift;
    my $element   = shift;
    my $criterion = sub { $_ eq $element };
    return $self->grep_elements($criterion);
}

has current_constraint_attr => (
    is  => 'rw',
    isa => HashRef,
);

has current_node => (
    is      => 'ro',
    writer  => 'set_current_node',
    clearer => 'clear_current_node',
    isa     => Str,
);

has current_constraint_values => (
    traits     => ['Array'],
    is         => 'ro',
    isa        => ArrayRef [Str],
    default    => sub { [] },
    auto_deref => 1,
    handles    => {
        clear_constraint_values => 'clear',
        add_constraint_value    => 'push',
        has_constraint_values   => 'count',
    },
);

has logic_string => (
    is  => 'rw',
    isa => Str,

);

sub start_document {
    my $self = shift;

    # so we can add logic and subclass constraints
    # without worrying about sequence
    $self->query->suspend_validation;
}

sub start_element {
    my $self  = shift;
    my $args  = shift;
    my $query = $self->query;
    $self->push_element( $args->{Name} );
    my $nameattr = $args->{Attributes}{name};

    # There are 6 basic possible elements to a basic query:
    # query, pathDescription, node, constraint, value, and join

    # Query
    if ( $args->{Name} eq 'query' ) {
        confess "We have two names and they differ"
          if (  $query->name
            and $nameattr
        # we can't test for equality due to the effect of coercion
            and length($query->name) != length($nameattr) );
        $query->name($nameattr) if $nameattr;
        confess "Model name is not suitable for this service"
          if ( $query->model->model_name ne $args->{Attributes}{model} );
        my $view = $args->{Attributes}{view};
        my @views = split( /[\s,]/, $view );
        confess 'No view in query' unless @views;
        for (@views) {
            if (/:/) {    # Old style join
                my ( $before_colon, $after_colon ) = split(/:/);
                my ( $field, @rest ) = split( /\./, $after_colon );
                $query->add_join(
                    path  => join( '.', $before_colon, $field ),
                    style => 'OUTER',
                );
                $query->add_view(
                    join( '.', $before_colon, $field, @rest ) );
            } else {
                $query->add_view($_);
            }
        }
        if (my $so = $args->{Attributes}{sortOrder}) {
            $query->clear_sort_order;
            #assume all paths have directions
            if ($so =~ / (?:asc|desc)/) {
                my %path_dir = split(/\s/, $so);
                while (my ($p, $d) = each %path_dir) {
                    $query->add_sort_order($p, $d);
                }
            } else {
                my @paths = split(/\s/, $so);
                $query->add_sort_order($_) for @paths;
            }
        }
        my ( $already, $new ) =
          ( $query->description, $args->{Attributes}{longDescription} );
        confess
"There are two descriptions ($already and $new) and they differ"
          if ( $already and $new and $already ne $new );
        $query->description( $args->{Attributes}{longDescription} )
          if $args->{Attributes}{longDescription};
        $self->logic_string( $args->{Attributes}{constraintLogic} )
          if $args->{Attributes}{constraintLogic};
    }

    # pathDescription
    elsif ( $args->{Name} eq 'pathDescription' ) {
        my $path = $args->{Attributes}{pathString};
        $path =~ s/:/./g;
        $query->add_pathdescription(
            path        => $path,
            description => $args->{Attributes}{description},
        );
    }

    # Node
    elsif ( $args->{Name} eq 'node' ) {
        my $path = $args->{Attributes}{path};
        if ( $path =~ /:/ ) {    # add the outer join
            my ( @parts ) = split( /:/, $path );
            my $after_colon = pop(@parts);
            my $before_colon = join('.', @parts);
            my ( $field, @rest ) = split( /\./, $after_colon );
            $query->add_join(
                path  => join( '.', $before_colon, $field ),
                style => 'OUTER',
            );
            $path = join( '.', $before_colon, $field, @rest );
        }
        $self->set_current_node($path);
        $query->add_constraint(
            path => $path,
            type => $args->{Attributes}{type},
        );
    }

    # Join
    elsif ( $args->{Name} eq 'join' ) {
        $query->add_join(
            path  => $args->{Attributes}{path},
            style => $args->{Attributes}{style},
        );
    }

    # constraint
    elsif ( $args->{Name} eq 'constraint' ) {
        $self->current_constraint_attr( $args->{Attributes} );

    } elsif ( $args->{Name} ne 'value' )
    {    # values contain no attributes, but only character data
        confess "unexpected element: ", $args->{Name}, "\n";
    }
}

sub characters {
    my $self = shift;
    my $args = shift;
    if ( $self->within_element('value') ) {
        if ( $self->within_element('constraint') ) {
            $self->add_constraint_value( $args->{Data} );
        } else {
            confess
"encountered a value outside a constraint - don't know what to do with one of those";
        }
    }
}

sub end_element {
    my $self = shift;
    my $name = shift->{Name};
    if ( $name eq 'constraint' ) {
        my %args = $self->process_constraint_attr;
        $self->query->add_constraint(%args);
    } elsif ( $name eq 'node' ) {
        $self->clear_current_node;
    }
    $self->pop_element;
}

sub process_constraint_attr {
    my $self = shift;
    my $attr = $self->current_constraint_attr;
    confess
      "Constraint element cannot have a path attribute inside a node"
      if ( $attr->{path} and $self->current_node );
    my %args = ( path => ( $attr->{path} || $self->current_node ) );
    $args{code}        = $attr->{code}       if $attr->{code};
    $args{op}          = $attr->{op}         if $attr->{op};
    $args{extra_value} = $attr->{extraValue} if $attr->{extraValue};
    $args{value}       = $attr->{value}
      if ( exists $attr->{value} and $attr->{value} ne 'null' );
    $args{type} = $attr->{type} if $attr->{type};
    $args{loop_path} = $attr->{loopPath} if $attr->{loopPath};

    # Workarounds for legacy operators which may be present in old xml
    if ($args{op}) {
        if ($args{op} eq 'CONTAINS') {
            $args{op}    = '=';
            $args{value} = '*' . $args{value} . '*';
        } elsif ($args{op} eq 'LIKE') {
            $args{op} = '=';
        } elsif ($args{op} eq 'NOT LIKE') {
            $args{op} = '!=';
        }
    }

    if ( $self->has_constraint_values ) {
        $args{values} = [ $self->current_constraint_values ];
        $self->clear_constraint_values;
    }
    return %args;
}

sub end_document {
    my $self = shift;
    unless ($self->query->is_dubious) {
        $self->query->clean_out_SCCs;
        $self->query->clean_out_irrelevant_sort_orders;
        $self->query->resume_validation;
    }
    $self->query->set_logic( $self->logic_string ) if $self->logic_string;
    $self->query->validate unless $self->query->is_dubious;
}

__PACKAGE__->meta->make_immutable;
no Moose;
1;
