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
        elsif ($args->{Name} eq 'node') {
            $self->{current_node} = $args->{Attributes}{path};
#            $template->{paths}{$args->{Attributes}{path}}{type} = $args->{Attributes}{type};
        }
        elsif ($args->{Name} eq 'constraint') {
	    my %args;
	    $args{path}        = $self->{current_node};
	    $args{code}        = $args->{Attributes}{code};
	    $args{op}          = $args->{Attributes}{op};
	    $args{value}       = $args->{Attributes}{value} unless ($args->{Attributes}{value} eq 'null');
	    $args{extraValue}  = $args->{Attributes}{extraValue};
	    $args{editable}    = $args->{Attributes}{editable};
	    my $con = InterMine::PathQuery::Constraint->new(%args);
	    
	    push @{$template->{constraints}{$args{path}}}, $con;
        }
        elsif ($args->{Name} ne 'pathDescription') {
            die "unexpected element: ", $args->{Name}, "\n";
        }
    }
}

1; 
