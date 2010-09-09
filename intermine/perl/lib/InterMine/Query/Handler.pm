package InterMine::Query::Handler;

use Moose;
use InterMine::TypeLibrary qw(Query);
use MooseX::Types::Moose qw(HashRef ArrayRef Str);
use Carp qw/cluck/;

has query => (
    is  => 'rw',
    isa => Query,
);

has element_stack => (
    traits => ['Array'],
    is => 'ro',
    isa => ArrayRef[Str],
    default => sub { [] },
    handles => {
	push_element  => 'push',
        pop_element   => 'pop',
	grep_elements => 'grep',
	open_elements => 'elements',
    },
);

sub within_element {
    my $self	  = shift;
    my $element	  = shift;
    my $criterion = sub {$_ eq $element};
    return $self->grep_elements($criterion);
}

has current_constraint_attr => (
    is => 'rw',
    isa => HashRef,
);

has current_node => (
    is	    => 'ro',
    writer  => 'set_current_node',
    clearer => 'clear_current_node',
    isa	    => Str,
);

has current_constraint_values => (
    traits => ['Array'],
    is => 'ro',
    isa => ArrayRef[Str],
    default => sub { [] },
    auto_deref => 1,
    handles => {
	clear_constraint_values => 'clear',
	add_constraint_value => 'push',
	has_constraint_values => 'count',
    },
);

has logic_string => (
    is => 'rw',
    isa => Str,

);

sub start_document {
    my $self = shift;
    # so we can add logic and subclass constraints
    # without worrying about sequence
    $self->query->suspend_validation;
}

sub start_element {
    my $self = shift;
    my $args = shift;
    my $query = $self->query;
    $self->push_element($args->{Name});
    my $nameattr = $args->{Attributes}{name};
    # There are 6 basic possible elements to a basic query:
    # query, pathDescription, node, constraint, value, and join

    # Query
    if ($args->{Name} eq 'query') {
	confess "We have two names and they differ"
	    if ($query->name and $nameattr and $query->name ne $nameattr);
	$query->name($nameattr);
	confess if ($query->model->model_name ne $args->{Attributes}{model});
	$query->add_view($args->{Attributes}{view});
	$query->set_sort_order($args->{Attributes}{sortOrder})
	    if $args->{Attributes}{sortOrder};
	my ($already, $new) =
	    ($query->description, $args->{Attributes}{longDescription});
	confess "There are two descriptions ($already and $new) and they differ"
	    if ( $already and $new and $already ne $new);
	$query->description($args->{Attributes}{longDescription})
	    if $args->{Attributes}{longDescription};
	$self->logic_string($args->{Attributes}{constraintLogic})
	    if $args->{Attributes}{constraintLogic};
    }

    # pathDescription
    elsif ($args->{Name} eq 'pathDescription') {
	$query->add_pathdescription(
		     path        => $args->{Attributes}{pathString},
		     description => $args->{Attributes}{description},
		     );
    }

    # Node
    elsif ($args->{Name} eq 'node') {
	my $path = $args->{Attributes}{path};
	$self->set_current_node($path);
	$query->add_constraint(
			       path => $path,
			       type => $args->{Attributes}{type},
			      );
    }

    # Join
    elsif ($args->{Name} eq 'join') {
	$query->add_join(
			 path  => $args->{Attributes}{path},
			 style => $args->{Attributes}{style},
			);
    }

    # constraint
    elsif ($args->{Name} eq 'constraint') {
	$self->current_constraint_attr($args->{Attributes});

    }
    elsif ($args->{Name} ne 'value') { # values contain no attributes, but only character data
	confess "unexpected element: ", $args->{Name}, "\n";
    }
}

sub characters {
    my $self = shift;
    my $args = shift;
    if ($self->within_element('value')) {
	if ($self->within_element('constraint')) {
	    $self->add_constraint_value($args->{Data});
	}
	else {
	    confess "encountered a value outside a constraint - don't know what to do with one of those";
	}
    }
}

sub end_element {
    my $self = shift;
    my $name = shift->{Name};
    if ($name eq 'constraint') {
	my %args = $self->process_constraint_attr;;
	$self->query->add_constraint(%args);
    }
    elsif ($name eq 'node') {
	$self->clear_current_node;
    }
    $self->pop_element;
}

sub process_constraint_attr {
    my $self = shift;
    my $attr = $self->current_constraint_attr;
    confess "Constraint element cannot have a path attribute inside a node"
	if ($attr->{path} and $self->current_node);
    my %args = (path => ($attr->{path} || $self->current_node) );
    $args{code}        = $attr->{code} if $attr->{code};
    $args{op}          = $attr->{op}   if $attr->{op};
    $args{extra_value} = $attr->{extraValue} if $attr->{extraValue};
    $args{value}       = $attr->{value}
	if (exists $attr->{value} and $attr->{value} ne 'null');
    $args{type}        = $attr->{type} if $attr->{type};
    if ($self->has_constraint_values) {
	$args{values}  = [$self->current_constraint_values];
	$self->clear_constraint_values;
    }
    return %args;
}

sub end_document {
    my $self = shift;
    $self->query->clean_out_SCCs;
    $self->query->resume_validation;
    $self->query->logic($self->logic_string) if $self->logic_string;
    $self->query->validate;
}

1;
