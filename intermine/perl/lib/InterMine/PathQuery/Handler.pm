package InterMine::PathQuery::Handler;

use strict;
use warnings;
use Carp;

use InterMine::PathQuery::Constraint;

sub new {
    my $class = shift;
    my $type = shift;
    croak "No query type passed to new" unless $type;
    my $self = {type => $type}; 
    return bless $self, $class;
}

sub start_element {
    my $self = shift;
    my $args = shift;
    my $type = $self->{type};
    my $nameattr = $args->{Attributes}{name};
    if ($args->{Name} eq $type) {
	croak "No name attribute in the $type element" unless $nameattr;
	$self->{query}{name}  = $nameattr;
	$self->{query}{date_created} = $args->{Attributes}{'date-created'};
	$self->{query}{title} = $args->{Attributes}{title};
	if ($args->{Attributes}{description}) {
	    croak "Query has both description and title attributes" if $self->{query}{title};
	    $self->{query}{title} = $args->{Attributes}{description};
	}
	$self->{query}{longDescription} = $args->{Attributes}{longDescription};
	$self->{query}{comment} = $args->{Attributes}{comment};
    }
    else {
        my $query = $self->{query};
        if ($args->{Name} eq 'query') {
	    croak "Names for query and template differ" 
		if ($query->{name} and $nameattr and $query->{name} ne $nameattr);
            $query->{model_name} = $args->{Attributes}{model};
            my @views = ();
            if (exists $args->{Attributes}{view}) {
                @views = split(/\s+/, $args->{Attributes}{view});
            }
            $query->{view} = \@views;
	    $query->{sort_order} = $args->{Attributes}{sortOrder};
	    $query->{constraintLogic} = $args->{Attributes}{constraintLogic};
        }
	elsif ($args->{Name} eq 'pathDescription') {
	    $query->{pathDescriptions}{$args->{Attributes}{pathString}} =
		$args->{Attributes}{description};
	}
        elsif ($args->{Name} eq 'node') {
	    my $path = $args->{Attributes}{path};
            $self->{current_node} = $path;
	    $query->{constraints}{$path} = [];
	    $query->{type_of}{$path} = $args->{Attributes}{type};
        }
        elsif ($args->{Name} eq 'constraint') {
	    my %args = (
		path        => $self->{current_node},
		code        => $args->{Attributes}{code},
		op          => $args->{Attributes}{op},
		extraValue  => $args->{Attributes}{extraValue},
		editable    => $args->{Attributes}{editable},
		description => $args->{Attributes}{description},
		identifier  => $args->{Attributes}{identifier},
		);
	    $args{value}    = $args->{Attributes}{value} unless ($args->{Attributes}{value} eq 'null');

	    my $con = InterMine::PathQuery::Constraint->new(%args);
	    
	    push @{$query->{constraints}{$args{path}}}, $con;
        }
       	else {
            die "unexpected element: ", $args->{Name}, "\n";
        }
    }
}

1; 
