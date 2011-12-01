package Webservice::InterMine::Simple::Query;

use strict;
use URI;

use constant 'RESOURCE_PATH' => '/query/results';

=head1 NAME

Webservice::InterMine::Simple::Query

=head1 SYNOPSIS

  my $query = $service->new_query;

  $query->add_view("Organism.shortName", "Organism.taxonId");
  $query->add_constraint({path => "Organism.genus", op => "=", value => "Rattus"});

  @rows = $query2->results_table;
  for my $row (@rows) {
    print "name: $row->[0], id: $row->[1]\n";
  }

=head1 DESCRIPTION

This is a basic representation of a query. It can handle straight-forward
requests and result sets, but for anything more complicated, we recommend you look
as the more fully featured L<Webservice::InterMine>. This is especially true of 
large data sets, as this implementation has the potential to use lots of memory when 
receiving and processing results.

=head1 METHODS

=head2 new - Construct a new query.

Create a new blank query.

=cut

sub new {
    my $class = shift;
    my $self = {@_};
    $self->{model} ||= "genomic"; 
    $self->{view}  ||= "";
    $self->{joins} ||= [];
    $self->{constraints} ||= [];
    return bless $self, $class;
}

=head2 new_from_xml - Construct a new query from xml

Read in an existing query from a string or a file.

Parameters:

=over

=item * source_file => $file_name

The name of the file to read in.

=item * source_string => $xml

The xml that represents the query.

=back

=cut

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

=head2 add_view($col1, $col2)

Add one or more output columns to the query. These must be fully qualified,
legal path-strings. No validation will be performed.

=cut

sub add_view {
    my $self = shift;
    die "Cannot alter a query you have read in from xml" if $self->{xml};
    $self->{view} .= join(' ', @_);
}

=head2 add_constraint([ $href | %parameters])

Add a constraint to the query. The constraint may be represented either as 
a hash-reference, or as a list of parameters.

  $query->add_constraint(path => "Organism.species", op => "=", value => "melanogaster")

=cut

sub add_constraint {
    my $self = shift;
    die "Cannot alter a query you have read in from xml" if $self->{xml};
    my $constraint = (ref $_[0]) ? shift : {@_};
    push @{$self->{constraints}}, $constraint;
}

=head2 add_join([ $href | %parameters])

Add a join to the query. The join may be represented either as 
a hash-reference, or as a list of parameters.

  $query->add_join(path => "Gene.proteins", style => "OUTER")

=cut

sub add_join {
    my $self = shift;
    die "Cannot alter a query you have read in from xml" if $self->{xml};
    my $join = (ref $_[0]) ? shift : {@_};
    push @{$self->{joins}}, $join;
}

=head2 set_logic($logic)

Set the constraint logic of the query. The logic must be represented as a 
legal logic string. No validation will be done.

=cut

sub set_logic {
    my $self = shift;
    die "Cannot alter a query you have read in from xml" if $self->{xml};
    $self->{logic} = shift;
}

=head2 set_sort_order(@list_of_elements)

Set the sort order for the query. The sort order should be composed of pairs
of paths and directions:

  $query->set_sort_order("Organism.genus asc Organism.species desc");

=cut

sub set_sort_order {
    my $self = shift;
    die "Cannot alter a query you have read in from xml" if $self->{xml};
    $self->{sort_order} = join(' ', @_);
}

sub _get_uri {
    my $self = shift;
    my $uri = URI->new($self->{service}{root} . RESOURCE_PATH);
    return $uri;
}

my %safe_version_of = (
    '='  => '=',
    'eq' => '=',
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

=head2 as_xml - get the XML serialisation of the query

This is either the same value passed in to new_from_xml, or 
a very naïve XML serialisation. No thorough XML escaping will be performed.

=cut

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
        $xml .= qq(code="$constraint->{code}" ) if $constraint->{code};
        $xml .= ">";
        if ($values) {
            $xml .= join('', map {"<value>$_</value>"} @$values);
        }
        $xml .= "</constraint>";
    }
    $xml .= "</query>";
    return $xml;
}

=head2 results - get the results for this query as a single string.

Returns the string representation of the query's results.

Parameters:

=over

=item * as => $format

Specifies the result format.

=item * size => int

Specifies the maximum number of rows to return.
A query can return up to 10,000,000 rows in a single page.

=item * start => int

Specifies the index of the first result to return.

=item * columnheaders => bool

Whether or not you want the first row to be the names
of the output columns.

=back

=cut

sub results {
    my $self = shift;
    my %args = @_;
    my $uri  = $self->_get_uri;
    my %query_form = (query => $self->as_xml, format => $args{as});
    for (qw/size start columnheaders/) {
        $query_form{$_} = $args{$_} if (exists $args{$_});
    }
    if ($self->{service}{token}) {
        $query_form{token} = $self->{service}{token};
    }
    $uri->query_form(%query_form);
    my $result = $self->{service}{ua}->get($uri);
    if ($result->is_success) {
        return $result->decoded_content; 
    } else {
        die $result->status_line, "\n", $result->decoded_content;
    }
}

=head2 results_table - get a list of rows (as array-references)

Performs very naïve parsing of returned tabular data and splits 
rows into array references based using a failure-prone "tab" split. 

=cut

sub results_table {
    my $self = shift;
    my $results = $self->results(as => 'tab', @_);
    my @lines = map {[split /\t/]} split(/\n/, $results);
    return @lines;
}

=head2 get_count - get the number of rows the query returns

Returns a number representing the total number of rows in the result set.

=cut

sub get_count {
    my $self = shift;
    return $self->results(as => 'count') + 0;
}

=head1 SEE ALSO

=over 4

=item * L<Webservice::InterMine> For a more powerful alternative

=back

=head1 AUTHOR

Alex Kalderimis C<< <dev@intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine

You can also look for information at:

=over 4

=item * InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/perlapi>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut

1;
    
        
