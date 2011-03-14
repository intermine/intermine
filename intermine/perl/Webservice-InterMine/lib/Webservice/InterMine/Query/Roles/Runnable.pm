package Webservice::InterMine::Query::Roles::Runnable;

use Moose::Role;
requires qw(view service model get_request_parameters resource_path);

use MooseX::Types::Moose qw(Str);
use Perl6::Junction qw/any/;
use JSON -support_by_pp, -no_export;
use Webservice::InterMine::Service;
use Webservice::InterMine::ResultObject;

has json_parser => (
    is         => 'ro',
    isa        => 'JSON',
    lazy_build => 1,
);

sub _build_json_parser {
    return JSON->new->allow_singlequote->allow_barekey;
}

use constant DEFAULT_FORMAT => 'tab';

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

=cut

sub results_iterator {
    my $self  = shift;
    my %args  = @_;
    my $roles = delete $args{with};
    return $self->service->get_results_iterator(
        $self->url( %args ),
        $self->view, $roles, 
    );
}

my @json_format = (qw/jsonobjects jsonrows/);
my @simple_formats = (qw/ string count /);
my @valid_formats = (qw/ strings arrayref arrayrefs hashref hashrefs /, 
    @json_format, @simple_formats);

=head2 results( %options )

returns the results from a query in the result format
specified. 

The following options are available:

=over 4

=item * as => $format

Possible values: (string|strings|arrayrefs|hashrefs|jsonobjects|jsonrows|count)

The format to request results in. The default is C<arrayrefs>

=item * size => $size

The number of results to return. Leave undefined for "all" (default).

=item * start => $start 

The first result to return (starting at 0). The default is 0.

=item * json => $json_processor

Possible values: (inflate|instantiate|raw|perl)

What to do with JSON results. The results can be returned as inflated objects,
full instantiated Moose objects, a raw json string, or as a perl
data structure. (default is C<perl>).

=back

=cut

sub results {
    my $self   = shift;
    my %args   = @_;
    my $wanted = $args{as} || 'arrayref';    # string and hashref are possible
    confess "Invalid format selected" unless ($wanted eq any(@valid_formats));

    my $format = $self->DEFAULT_FORMAT;
    if ( $wanted eq any(@json_format, 'count') ) {
        $format = $wanted;
        $wanted = 'string';
    }
        
    $wanted =~ s/s$//;    # trim trailing 's' on arrayrefs/hashrefs
    my $i     = $self->results_iterator(
        format => $format,
        start => $args{start},
        size  => $args{size},
        addheaders => $args{addheaders},
    );
    my @lines = $i->all_lines($wanted);
    if ( $wanted eq 'string' and $args{as} eq any(@simple_formats) ) {
        return join( "\n", @lines );
    }
    elsif ( $wanted eq 'string' and $args{as} eq any(@json_format) ) {
        $self->_handle_json_results( \@lines, lc( $args{json} ) );
    }
    else {
        return \@lines;
    }
}

##
# _handle_json_results(\@lines, $json_format)
#
##

sub _handle_json_results {
    my ( $self, $lines, $json ) = @_;
    my $json_text = join( '', @$lines );

    return $json_text if ( $json eq 'raw' );

    my $perl = $self->json_parser->decode($json_text)->{results};

    if ( $json eq 'inflate' ) {
        return inflate($perl);
    }
    elsif ( $json eq 'instantiate' ) {
        return [ map { $self->model->make_new($_) } @$perl ];
    }
    else {
        return $perl;
    }
}

=head2 url 

Get the url for a webservice request

=cut

sub url {
    my $self = shift;
    my %args = @_;
    my %query_form = $self->get_request_parameters;
    $query_form{format} = $args{format} || 'tab';
    
    # Set optional parameters
    for my $opt (qw/start size columnheaders/) {
        $query_form{$opt} = $args{$opt} if ($args{$opt});
    }

    my $url = $self->service->root . $self->resource_path;
    my $uri = URI->new($url);
    $uri->query_form(%query_form);
    return $uri;
}

1;

=head1 FUNCTIONS

=head2 inflate( thing )

Inflates the thing passed in, blessing hashes into Webservice::InterMine::ResultObjects, 
and recursing through their values, and iterating over their arrays.

=cut

sub inflate {
    my $thing = shift;
    my $type  = ref $thing;
    if ( $type eq "HASH" ) {
        bless $thing, "Webservice::InterMine::ResultObject";
        for my $sub_thing ( values %$thing ) {
            inflate($sub_thing);
        }
    }
    elsif ( $type eq "ARRAY" ) {
        for my $sub_thing (@$thing) {
            inflate($sub_thing);
        }
    }
    return $thing;
}

sub save {

    # saves queries as saved_queries, and templates as updated templates
    confess "NOT IMPLEMENTED YET";
    my $self = shift;
    my %args = @_;
    $self->name( $args{name} ) if ( exists $args{name} );
    my $xml  = $self->to_xml;
    my $url  = $self->upload_url;
    my $resp = $self->service->send_off( $xml => $url );
    confess "Failed to save data to webservice"
      unless $resp->is_success;
    return;
}

sub save_as_template {    # saves queries as templates,
                          # with all template constraint attributes
                          # set to defaults values - can be used to save
                          # a modified template under a new name
    confess 'NOT IMPLEMENTED YET';    # TODO
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
