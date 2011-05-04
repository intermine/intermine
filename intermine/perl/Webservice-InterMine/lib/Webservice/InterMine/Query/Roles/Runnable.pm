package Webservice::InterMine::Query::Roles::Runnable;

use MooseX::Role::WithOverloading;
requires qw(view service model get_request_parameters resource_path
            upload_path to_xml validate);

use MooseX::Types::Moose qw(Str);
use Perl6::Junction qw/any/;
use Webservice::InterMine::Service;
use Webservice::InterMine::Types qw(ResultIterator);
use Webservice::InterMine::ResultObject;
use IO::Handle;

use constant {
    DEFAULT_FORMAT => 'tab',
};

use overload (
    '<>' => 'next_result',
    fallback => 1,
);

around BUILDARGS => sub {
    my $orig  = shift;
    my $class = shift;
    if ( @_ == 1 && !ref $_[0] ) {
        my $service = Webservice::InterMine::Service->new( root => $_[0] );
        my $model = $service->model;
        return $class->$orig(
            service => $service,
            model   => $model,
        );
    }
    else {
        return $class->$orig(@_);
    }
};


=head1 NAME 

Webservice::InterMine::Query::Roles::Runnable - Composable behaviour for runnable queries

=head1 DESCRIPTION 

This module provides composable behaviour for running a query against a webservice and getting the results.

=head1 METHODS

=head2 results_iterator 

Returns a results iterator for use with a query.

The following options are available:

=over 4

=item * as => $format

Possible values: (string|tsv|csv|arrayrefs|hashrefs|jsonobjects|jsonrows|count)

The format to request results in. The default is C<arrayrefs>

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

Possible values: (inflate|instantiate|raw|perl)

What to do with JSON results. The results can be returned as inflated objects,
full instantiated Moose objects, a raw json string, or as a perl
data structure. (default is C<perl>).

=back

=cut

has _iterator => (
    is => 'ro',
    isa => ResultIterator,
    lazy_build => 1,
    builder => 'results_iterator',
);

sub next_result {
    my $self = shift;
    my $next = $self->_iterator->next;
    $self->_clear_iterator unless (defined $next);
    return $next;
}

sub results_iterator {
    my $self  = shift;
    my %args  = @_;

    $self->validate;

    my $row_format  = delete($args{as})   || "arrayrefs";
    $row_format = 'tsv' if ($row_format eq 'string');
    my $json_format = delete($args{json}) || "perl";
    my $roles       = delete $args{with};

    my %query_form = $self->get_request_parameters;
    
    # Set optional parameters
    for my $opt (qw/start size columnheaders/) {
        $query_form{$opt} = $args{$opt} if (defined $args{$opt});
    }
    return $self->service->get_results_iterator(
        $self->url,
        \%query_form,
        $self->view, 
        $row_format, 
        $json_format,
        $roles, 
    );
}

=head2 results( %options )

returns the results from a query in the result format
specified. 

The following options are available:

=over 4

=item * as => $format

Possible values: (tsv|csv|arrayrefs|hashrefs|jsonobjects|jsonrows|count)

The format to request results in. The default is C<arrayrefs>

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
data structure. (default is C<perl>).

=back

=cut

sub results {
    my $self = shift;
    my $iter = $self->results_iterator(@_);
    return $iter->get_all();
}

=head2 print_results( %options )

returns the results from a query in the result format
specified. 

The following options are available:

=over 4

=item * to => $file|GlobRef|<does print>

A file name to open, or a file handle opened for writing, or 
an object that can print.

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
data structure. (default is C<perl>).

=back

=cut

sub print_results {
    my $self = shift;
    my %args = @_;
    my $to = delete($args{to}) or confess "No 'to' option supplied to print_results";
    $args{as} ||= 'tsv'; # For printing, we default to TSV.
    my $out; 
    if ($to) {
        if (ref $to eq 'GLOB') {
            $out = $to;
        } elsif (blessed($to) and $to->can('print')) {
            $out = $to;
        } else {
            open($out, '>:utf8', $to) or confess "Cannot open $to, $!";
        }
    } else {
        $out = \*STDOUT;
    }
    my $iter = $self->results_iterator(%args);
    while (my $line = <$iter>) {
        $out->print($line, "\n");
    }
}

sub get_count {
    my $self = shift;
    my $iter = $self->results_iterator(as => "count");
    return join('', $iter->get_all());
}

=head2 url 

Get the url for a webservice resource.

=cut

sub url {
    my $self = shift;
    my $url = $self->service->root . $self->resource_path;
    my $uri = URI->new($url);
    return $uri;
}

sub get_upload_url {
    my $self = shift;
    my $url = return $self->service->root . $self->upload_path;
    my $uri = URI->new($url);
    return $uri;
}

sub save {
    my $self = shift;
    my %args = @_;
    $self->validate;
    $self->name( $args{name} ) if ( exists $args{name} );
    my $xml  = $self->to_xml;
    my $url  = $self->get_upload_url;
    my $resp = $self->service->send_off( $xml => $url );
    my $name = $self->name;
    my $root = $self->service->root;
    unless ($resp =~ /$name\tsuccess/i) {
        confess "Error saving $name to your account at $root:\n$resp";
    }
    return;
}

1;

=head1 SEE ALSO

=over 4

=item * L<Webservice::InterMine::Cookbook> for guide on how to use these modules.

=item * L<Webservice::InterMine::Query>

=item * L<Webservice::InterMine::Service>

=item * L<Webservice::InterMine::Query::Template>

=back

=head1 AUTHOR

Alex Kalderimis C<< <dev@intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::Query::Roles::Runnable

You can also look for information at:

=over 4

=item * Webservice::InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/perlapi>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut
