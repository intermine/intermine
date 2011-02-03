package Webservice::InterMine::Query::Roles::Runnable;

use Moose::Role;
requires qw(view service url);

use MooseX::Types::Moose qw(Str);
use Perl6::Junction qw/any/;
use JSON -support_by_pp, -no_export;
use Webservice::InterMine::Service;
use Webservice::InterMine::ResultObject;

has json_parser => (
    is  => 'ro',
    isa => 'JSON',
    lazy_build => 1,
);

sub _build_json_parser {
    return JSON->new->allow_singlequote->allow_barekey;
}

has format => (
    is => 'rw',
    isa => Str,
    default => 'tab',
);

around BUILDARGS => sub {
    my $orig  = shift;
    my $class = shift;
    if ( @_ == 1 && !ref $_[0] ) {
        my $service =
          Webservice::InterMine::Service->new( root => $_[0] );
        my $model = $service->model;
        return $class->$orig(
            service => $service,
            model   => $model,
        );
    } else {
        return $class->$orig(@_);
    }
};
=head1 NAME Webservice::InterMine::Query::Roles::Runnable - Composable behaviour for runnable queries

=head1 DESCRIPTION 

This module provides composable behaviour for running a query against a webservice and getting the results.

=head1 METHODS

=head2 results_iterator 

Returns a results iterator for use with a query.

=cut

sub results_iterator {
    my $self  = shift;
    my %args  = @_;
    my $roles = $args{with} if ( exists $args{with} );
    return $self->service->get_results_iterator( 
        $self->url(format => $self->format), $self->view, $roles, );
}

my @json_format = (qw/jsonobjects jsonrows/);

=head2 results(as => $format, inflate => true|false)

returns the results from a query in the result format
specified. If the inflate option is given and true, then
jsonobjects returned will be inflated.

=cut

sub results {
    my $self   = shift;
    my %args   = @_;
    my $wanted = $args{as} || 'arrayref';  # string and hashref are possible
    if ($wanted eq any(@json_format)) {
        $self->format($wanted);
        $wanted = 'string';
    }
    $wanted =~ s/s$//;  # trim trailing 's' on arrayrefs/hashrefs
    my $i     = $self->results_iterator;
    my @lines = $i->all_lines($wanted);
    if ( $wanted eq 'string' and $args{as} eq 'string' ) {                   
        #ie. the original wanted was "string"
        return join( "\n", @lines );
    } elsif ( $wanted eq 'string' and $args{as} eq any(@json_format) ) {
        my $obj = $self->json_parser->decode(join('', @lines));
        my $ret = $obj->{results};
        if ($args{inflate}) {
            inflate($ret);
        }
        return $ret;
    } else {
        return \@lines;
    }
}

=head1 FUNCTIONS

=head2 inflate( thing )

Inflates the thing passed in, blessing hashes into Webservice::InterMine::ResultObjects, 
and recursing through their values, and iterating over their arrays.

=cut

sub inflate {
    my $thing = shift;
    my $type = ref $thing;
    if ($type eq "HASH") {
        bless $thing, "Webservice::InterMine::ResultObject";
        for my $sub_thing (values %$thing) {
            inflate($sub_thing);
        }
    } elsif ($type eq "ARRAY") {
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
