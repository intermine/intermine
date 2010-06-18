package InterMine::Template::Handler;

use strict;
use warnings;

use InterMine::PathQuery::Constraint;

sub new {
    my $class = shift;
    my $self = {}; 
    return bless $self, $class;
}

sub start_element {
    my $self = shift;
    my $args = shift;
    
    my $nameattr = $args->{Attributes}{name};
    if ($args->{Name} eq 'template') {
	$self->{template}{name}  = $nameattr;
	$self->{template}{title} = $args->{Attributes}{title};
	$self->{template}{longDescription} = $args->{Attributes}{longDescription};
	$self->{template}{comment} = $args->{Attributes}{comment};
    }
    else {
        my $template = $self->{template};
        if ($args->{Name} eq 'query') {
            $template->{model} = $args->{Attributes}{model};
            my @views = ();
            if (exists $args->{Attributes}{view}) {
                @views = split(/\s+/, $args->{Attributes}{view});
            }
            $template->{view} = \@views;
	    $template->{sort_order} = $args->{Attributes}{sortOrder};
	    $template->{constraintLogic} = $args->{Attributes}{constraintLogic};
        }
	elsif ($args->{Name} eq 'pathDescription') {
	    $template->{pathDescriptions}{$args->{Attributes}{pathString}} =
		$args->{Attributes}{description};
	}
        elsif ($args->{Name} eq 'node') {
	    my $path = $args->{Attributes}{path};
            $self->{current_node} = $path;
	    $template->{constraints}{$path} = [];
	    $template->{type_of}{$path} = $args->{Attributes}{type};
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
	    
	    push @{$template->{constraints}{$args{path}}}, $con;
        }
       	else {
            die "unexpected element: ", $args->{Name}, "\n";
        }
    }
}

1; 
