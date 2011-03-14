package Webservice::InterMine::Simple::Query;

use strict;
use URI;
use Encode;

use constant 'RESOURCE_PATH' => '/query/results';

sub new {
    my $class = shift;
    my $self = {@_};
    $self->{model} ||= "genomic"; 
    $self->{view}  ||= "";
    $self->{joins} ||= [];
    $self->{constraints} ||= [];
    return bless $self, $class;
}

sub new_from_xml {
    my $class = shift;
    my $self = $class->new(@_);
    if (my $file = $self->{source_file}) {
        open (my $xml_fh, '<', $file) or die "Could not open $file, $!";
        $self->{xml} = join('', <$xml_fh>);
        close $xml_fh or die "Could not close $file, $!";
    } else {
        $self->{xml} = $self->{source_string} or die "No xml source supplied";
    }
    return $self;
}

sub add_constraint {
    my $self = shift;
    my $constraint = (ref $_[0]) ? shift : {@_};
    push @{$self->{constraints}}, $constraint;
}

sub add_join {
    my $self = shift;
    my $join = (ref $_[0]) ? shift : {@_};
    push @{$self->{joins}}, $join;
}

sub add_view {
    my $self = shift;
    $self->{view} .= join(' ', @_);
}

sub set_logic {
    my $self = shift;
    $self->{logic} = shift;
}

sub set_sort_order {
    my $self = shift;
    $self->{sort_order} = join(' ', @_);
}

sub get_uri {
    my $self = shift;
    my $uri = URI->new($self->{service}{root} . RESOURCE_PATH);
    return $uri;
}

my %safe_version_of = (
    '='  => '&eq;',
    'eq' => '&eq;',
    '>'  => '&gt;',
    'gt' => '&gt;',
    '<'  => '&lt;',
    'lt' => '&lt;',
    '!=' => '!=',
    'ne' => '!=', 
    '>=' => '&ge;',
    'ge' => '&ge;',
    '<=' => '&le;',
    'le' => '&le;',
    'ONE OF' => 'ONE OF',
    'one of' => 'ONE OF',
    'NONE OF' => 'NONE OF',
    'none of' => 'NONE OF',
    'IS'      => 'IS',
    'is'      => 'IS', 
    'IS NOT'  => 'IS NOT',
    'is not'  => 'IS NOT',
    'isnt'    => 'IS NOT',
    'LOOKUP'  => 'LOOKUP',
    'lookup'  => 'LOOKUP',
);

sub as_xml {
    my $self = shift;
    return $self->{xml} if $self->{xml};
    my $xml = qq(<query model="$self->{model}" view="$self->{view}" );
    $xml .= qq(sortOrder="$self->{sort_order}" ) if $self->{sort_order};
    $xml .= qq(constraintLogic="$self->{logic}" ) if $self->{logic};
    $xml .= ">";
    for my $join (@{$self->{joins}}) {
        $xml .= qq(<join path="$join->{path}" style="$join->{style}"/>);
    }
    for my $constraint (@{$self->{constraints}}) {
        my $op = $safe_version_of{$constraint->{op}} if $constraint->{op};
        my $values = delete $constraint->{values};
        $xml .= qq(<constraint path="$constraint->{path}" );
        $xml .= qq(type="$constraint->{type}" ) if $constraint->{type};
        $xml .= qq(op="$op" ) if $op;
        $xml .= qq(value="$constraint->{value}" ) if $constraint->{value};
        $xml .= qq(extraValue="$constraint->{extra_value}" ) if $constraint->{extra_value};
        $xml .= ">";
        if ($values) {
            $xml .= join('', map {"<value>$_</value>"} @$values);
        }
        $xml .= "</constraint>";
    }
    $xml .= "</query>";
    return $xml;
}

sub results {
    my $self = shift;
    my %args = @_;
    my $uri  = $self->get_uri;
    my %query_form = (query => $self->as_xml, format => $self->{as});
    for (qw/size start addheader/) {
        $query_form{$_} = $args{$_} if (exists $args{$_});
    }
    $uri->query_form(%query_form);
    my $result = $self->{service}{ua}->get($uri);
    if ($result->is_success) {
        return encode_utf8($result->content);
    } else {
        die $result->status_line, "\n", encode_utf8($result->content);
    }
}

1;
    
        
