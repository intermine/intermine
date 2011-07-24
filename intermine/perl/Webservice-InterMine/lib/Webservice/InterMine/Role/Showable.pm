=head1 NAME

Webservice::InterMine::Role::Showable - behaviour for queries that can print their results out

=head1 SYNOPSIS

    use Webservice::InterMine;
    my $service  = Webservice::InterMine->new_service('www.flymine.org/query');

    # Print out a readable table of all results
    $query = $service->new_query(class => 'Gene');
    $query->select('*', 'pathways.*')
          ->where(symbol => [qw/bib eve h zen/])
          ->show();

    # Just print the first 20 rows
    $query = $service->new_query(class => 'Gene');
    $query->select('*', 'pathways.*')
          ->show_first(20);

    # Print the results out to a file.
    $query->print_results(columnheaders => 1, to 'some/file.tsv');

=head1 DESCRIPTION

Print out the results to either the screen (by default) or any 
arbitrary file or filehandle. These methods are used by queries 
and lists to diplay their contents for inpection and storage.

=cut

package Webservice::InterMine::Role::Showable;

use Moose::Role;

requires qw/to_string views results_iterator/;

use IO::Handle;
use List::Util qw(max);

=head2 show( [$fh, $no_of_rows] )

Print out the results to standard out (or an optional filehandle)
in a easy to read summary table format, with an informative header, 
column headers in the form of the views, and the results aligned
in columns.

=cut

sub show {
    my $self = shift;
    my $fh = shift || \*STDOUT;
    my $size = shift;

    binmode $fh, ':encoding(utf8)';
    print $fh $self->to_string, "\n";
    print $fh $self->_bar_line;
    printf $fh $self->_table_format, $self->views;
    print $fh $self->_bar_line;
    my $iter = $self->results_iterator(size => $size);
    while (<$iter>) {
        printf $fh $self->_table_format, map {(defined $_) ? $_ : 'UNDEF'} @$_;
    }
}

=head2 show_first($no_of_rows)

Prints out the first C<$no_of_rows> rows, or 10 rows if no argument was given
in the same format as C<show>.

=cut

sub show_first {
    my $self = shift;
    my $size = shift || 10;
    my $fh = shift || \*STDOUT;
    $self->show($fh, $size);
}

# Private methods for formatting tables

sub _bar_line {
    my $self = shift;
    my @view_lengths = map {length} $self->views;
    my $line = join('-+-', map { '-' x $_ } @view_lengths);
    return $line . "\n";
}

sub _table_format {
    my $self = shift;
    my @view_lengths = map {length} $self->views;
    my $format = join(' | ', map {'%-' . $_ . 's'} @view_lengths);
    return $format . "\n";
}

=head2 print_results( %options )

returns the results from a query in the result format
specified. 

The following options are available:

=over 4

=item * to => $filename|GlobRef

A file name to open, or a file handle opened for writing.

=item * as => $format

Possible values: (tsv|csv|arrayrefs|hashrefs|jsonobjects|jsonrows|count)

The format to print results in. The default is C<tsv>

=item * size => $size

The number of results to return. Leave undefined for "all" (default).

=item * start => $start 

The first result to return (starting at 0). The default is 0.

=item * addheaders => 0/1/friendly/path

Whether to return the column headers at the top of TSV/CSV results. The default is
false. There are two styles - friendly: "Gene > pathways > name" and 
path: "Gene.pathways.name". The default style is friendly if a true value is entered and
it is not "path".

=item * json => $json_processor

Possible values: (inflate|instantiate|perl)

What to do with JSON results. The results can be returned as inflated objects,
full instantiated Moose objects, a raw json string, or as a perl
data structure. (default is C<perl>). B<THIS OPTION MAKES LITTLE SENSE WHEN 
PRINTING RESULTS.>

=back

=cut

sub print_results {
    my $self = shift;
    my %args = @_;
    my $to = delete($args{to}) || \*STDOUT;
    $args{as} ||= 'tab'; # For printing, we default to TSV.
    my $out; 
    if ($to) {
        if (ref $to eq 'GLOB') {
            $out = $to;
        } else {
            open($out, '>', $to) or confess "Cannot open $to, $!";
        }
    } else {
        $out = \*STDOUT;
    }
    binmode $out, ':encoding(utf8)';
    my $iter = $self->results_iterator(%args);
    while (my $line = <$iter>) {
        $out->print($line, "\n");
    }
}

1;

=head1 SEE ALSO

=over 4

=item * L<Webservice::InterMine::Cookbook> for a guide on how to use these modules.

=item * L<Webservice::InterMine::Query>

=item * L<Webservice::InterMine::List>

=item * L<Webservice::InterMine::Query::Template>

=back

=head1 AUTHOR

Alex Kalderimis C<< <dev@intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::Role::Showable

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
