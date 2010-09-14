
=head1 NAME

Webservice::InterMine::Service - an object representation of an Webservice::InterMine Web-Service

=head1 SYNOPSIS

  use Webservice::InterMine;

  my $service = Webservice::InterMine->get_service('www.flymine.org/query/service');

  my $query    = $service->new_query;
  my $template = $service->template('Probe_Genes')
      or die "Cannot find template"

  # ... do stuff with your query/template

=head1 DESCRIPTION

The service object is the portal to the webservice: it provides the model
and templates for the objects you want to construct, and is used by the queries
to return results. Generally you won't need to interact with it directly, or
if you do, the methods you will be most interested in are those that return
objects you can use for running queries.

=head1 METHODS

=cut

package Webservice::InterMine::Service;

use Moose;
with 'Webservice::InterMine::Role::ModelOwner';
use Webservice::InterMine::Query;
use Webservice::InterMine::ResultIterator;
use Net::HTTP;
use URI;
use LWP::UserAgent;
use MooseX::Types::Moose qw/Str/;
use InterMine::TypeLibrary
  qw(Uri Model TemplateFactory SavedQueryFactory ListFactory);

use IO::String;

=head2 new( $url, [$user, $pass] )

A service can be constructed directly by passing a webservice
url to the new method. To have access to private data (personal
templates, saved queries and lists) you also need to provide
login information in the form of a username and password.
(B<AUTHENTICATION NOT IMPLEMENTED YET>).

=cut

around BUILDARGS => sub {
    my $orig  = shift;
    my $class = shift;
    if ( @_ <= 3 and $_[0] ne 'root' ) {
        my %args;
        $args{root} = shift if @_;
        $args{user} = shift if @_;
        $args{pass} = shift if @_;
        return $class->$orig(%args);
    } else {
        return $class->$orig(@_);
    }
};

use constant {
    QUERY_PATH         => '/query/results',
    MODEL_PATH         => '/model',
    TEMPLATES_PATH     => '/templates/xml',
    TEMPLATEQUERY_PATH => '/template/results',
    VERSION_PATH       => '/version',
    USER_AGENT         => 'WebserviceInterMinePerlAPIClient',
    LIST_PATH          => '/lists/xml',
    SAVEDQUERY_PATH    => '/savedqueries/xml',
};

=head2 root | user | pass

The values passed into the constructor can be accessed
via these methods. Note that the url passed in will have
a scheme added if none was provided.

=cut

has root => (
    is       => 'ro',
    isa      => Uri,
    required => 1,
    coerce   => 1,
    handles  => { host => 'host', },
);

has user => (
    is  => 'ro',
    isa => Str,
);

has pass => (
    is  => 'ro',
    isa => Str,
);

=head2 new_query

This returns a new query object for you to define
by adding constraints and a view to.

=cut

sub new_query {
    my $self = shift;
    my %args = @_;

    my $roles = $args{with};

    my $query = Webservice::InterMine::Query->new(
        service => $self,
        model   => $self->model,
    );
    return apply_roles( $query, $roles );
}

=head2 template( $name [$roles] )

This checks to see if there is a template of this name in the
webservice, and returns it to you if it exists.

=cut

has _templates => (
    is      => 'ro',
    isa     => TemplateFactory,
    coerce  => 1,
    lazy    => 1,
    default => sub {
        my $self = shift;
        return [
            $self, $self->model,
            $self->fetch( $self->root . TEMPLATES_PATH ),
        ];
    },
    handles => { get_template => 'get_template_by_name', },
);

sub template {
    my ( $self, $name, $roles ) = @_;
    my $t = $self->get_template($name) or return;
    return apply_roles( $t, $roles );
}

has _saved_queries => (
    is      => 'ro',
    isa     => SavedQueryFactory,
    coerce  => 1,
    lazy    => 1,
    default => sub {
        my $self = shift;
        $self->fetch( $self->root . SAVEDQUERY_PATH );
    },
    handles => { saved_query => 'get_saved_query_by_name', },
);
has _lists => (
    is      => 'ro',
    isa     => ListFactory,
    coerce  => 1,
    lazy    => 1,
    default => sub {
        my $self = shift;
        $self->fetch( $self->root . LIST_PATH );
    },
    handles => { list => 'get_list_by_name', },
);

=head2 model

returns the model for the webservice. This model is required
by many modules for checking validity with the webservice
database schema. See L<InterMine::Model>

=cut

sub _build_model {
    my $self = shift;
    $self->fetch( $self->root . MODEL_PATH );
}

=head2 version

Returns the version of the webservice - used for determining
compatibility with different query formats

=cut

has version => (
    is       => 'ro',
    isa      => Str,
    required => 1,
    lazy     => 1,
    default  => sub {
        my $self = shift;
        $self->fetch( $self->root . VERSION_PATH ),;
    },
);

# Returns a LWP::UserAgent suitable for getting and posting with
sub agent {
    my $ua = LWP::UserAgent->new;
    $ua->env_proxy;
    $ua->agent(USER_AGENT);

#    $ua->credentials($self->host.':80', $realm, $self->user, $self->pass);
    return $ua;
}

# Applies user supplied roles to object instances at runtime
sub apply_roles {
    my $instance = shift;
    my $roles    = shift;
    for (@$roles) {
        next unless ( defined $_ );

        # eval'ed to deal with the bareword rule
        eval "require $_";
        $_->meta->apply($instance);
    }
    return $instance;
}

# Returns a ResultIterator to process the query results with
sub get_results_iterator {
    my $self      = shift;
    my $url       = shift;
    my $view_list = shift;
    my $roles     = shift;

    # my $connection = Net::HTTP->new(Host => $self->host) or
    #   confess "Could not connect to host $@";
    # $connection->write_request(
    # 	GET => $url,
    # 	'User-Agent' => USER_AGENT
    # );
    my $resp    = $self->agent->get($url);
    my $content = $resp->content;
    open( my $io, '<', \$content ) or confess "$!";
    my $response = Webservice::InterMine::ResultIterator->new(

        #	connection => $connection,
        view_list     => $view_list,
        error_code    => $resp->code,
        error_message => $resp->message,
        content       => $io,
    );
    confess $response->status_line, $resp->content
      if $response->is_error;
    return apply_roles( $response, $roles );
}

# Fetch files from the webservice (used to fetch the model and the
# templates with - in the future also for saved queries and lists
sub fetch {
    my $self = shift;
    my $url  = shift;
    my $uri  = URI->new($url);
    $uri->query_form( format => 'text' );
    my $resp = $self->agent->get($uri);
    if ( $resp->is_error ) {
        confess $resp->status_line;
    } else {
        return $resp->content;
    }
}

# not designed to be accessed directly,
# but through the upload method on various objects
sub send_off {
    confess "NOT IMPLEMENTED YET";
    my $self = shift;
    my ( $xml, $url ) = @_;
    my $form = { xml => $xml, };
    my $resp = $self->agent->post( $url, $form );
    return $resp;
}

=head1 AUTHOR

Alex Kalderimis C<dev@intermine.org>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine

You can also look for information at:

=over 4

=item * Webservice::InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/perlapi>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2010 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut

1;
