package Webservice::InterMine::Simple::Template;

use strict;
use URI;

use constant 'RESOURCE_PATH' => '/template/results';

=head1 NAME

Webservice::InterMine::Simple::Template

=head1 SYNOPSIS

  my $template = $service->template("Gene_Pathways");

  @rows = $query2->results_table(constraint1 => "eq", value1 => "ABC trans*");
  for my $row (@rows) {
    print @$row;
  }

=head1 DESCRIPTION

This is a basic representation of a query. It can handle straight-forward
requests and result sets, but for anything more complicated, we recommend you look
as the more fully featured L<Webservice::InterMine>. This is especially true of 
large data sets, as this implementation has the potential to use lots of memory when 
receiving and processing results.

=head1 METHODS

=head2 new(name => $name) - Construct a new template.

Create an object representing the template with the given name.

=cut

sub new {
    my $class = shift;
    my $self = {@_};
    return bless $self, $class;
}

sub _get_uri {
    my $self = shift;
    my $uri = URI->new($self->{service}{root} . RESOURCE_PATH);
    return $uri;
}

=head2 results_with - get the results for this query as a single string.

Returns the string representation of the template's results.

Parameters:

=over 4

=item * %template_parameters

Specification of the template parameters to use. These take the form:

=over 8

=item constraint? => $path

=item op? => $op

=item value? => $value

=item extra? => $extra_value

=back

Where ? represents a number that is used to group properties for each template constraint.

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

sub results_with {
    my $self = shift;
    my %args = @_;
    my $uri = $self->_get_uri;
    my $format = delete $args{as} || 'tab';
    my %query_form = (name => $self->{name}, format => $format, %args);
    $uri->query_form(%query_form);
    my $result = $self->{service}{ua}->get($uri);
    if ($result->is_success) {
        return $result->content;
    } else {
        die $result->status_line, $result->content;
    }
}

=head2 results_table - get a list of rows (as array-references)

Performs very naïve parsing of returned tabular data and splits 
rows into array references based using a failure-prone "tab" split. 

Takes the same arguments as results_with.

=cut

sub results_table {
    my $self = shift;
    my $results = $self->results_with(@_);
    my @results = map {[ split /\t/ ]} split(/\n/, $results);
    return @results;
}

=head2 get_count - get the number of rows the query returns

Returns a number representing the total number of rows in the result set.

Takes the same arguments as results_with.

=cut

sub get_count {
    my $self = shift;
    my $results = $self->results_with(as => "count", @_);
    return $results + 0;
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
