package Webservice::InterMine::Service;

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

=cut

use Moose;
with 'Webservice::InterMine::Role::ModelOwner';
use Net::HTTP;
use URI;
use LWP;
use MIME::Base64;
use MooseX::Types::Moose qw/Str Int/;
use Webservice::InterMine::Types
  qw(ServiceVersion ServiceRootUri TemplateFactory SavedQueryFactory ListFactory);
use InterMine::Model::Types qw(Model);


=head2 new( $url, [$user, $pass] )

A service can be constructed directly by passing a webservice
url to the new method. To have access to private data (personal
templates, saved queries and lists) you also need to provide
login information in the form of a username and password.

=cut

around BUILDARGS => sub {
    my $orig  = shift;
    my $class = shift;
    my @build_args = @_;
    if ( @_ <= 3 and $_[0] ne 'root' ) {
        my %args;
        for (qw/root user pass/) {
            my $next = shift @build_args;
            $args{$_} = $next if $next;
        }
        return $class->$orig(%args);
    } else {
        return $class->$orig(@_);
    }
};

use constant {
    QUERY_PATH          => '/query/results',
    QUERY_SAVE_PATH     => '/query/upload',
    MODEL_PATH          => '/model/xml',
    TEMPLATES_PATH      => '/templates/xml',
    TEMPLATE_QUERY_PATH => '/template/results',
    TEMPLATE_SAVE_PATH  => '/template/upload',
    VERSION_PATH        => '/version',
    USER_AGENT          => 'WebserviceInterMinePerlAPIClient',
    LIST_PATH           => '/lists/json',
    SAVEDQUERY_PATH     => '/savedqueries/xml',
    RELEASE_PATH        => '/version/release',
};

=head1 CONSTRUCTION

=head2 Webservice::InterMine->get_service($root, $user, $pass)

Typically as service is most conveniently obtained through the
L<Webservice::InterMine> interface. 

=head2 new($root, $user, $pass)

It can of course be instantiated directly, with a standard call to new.

=head2 don't!

You do not have to obtain a service object: simply call the methods
on the Webservice::InterMine factory class to obtain new queries,
fetch templates and load saved queries.

=head1 METHODS

=head2 root | user | pass

The values passed into the constructor can be accessed
via these methods. Note that the url passed in will have
a scheme added if none was provided.

=cut

has root => (
    is       => 'ro',
    isa      => ServiceRootUri,
    required => 1,
    coerce   => 1,
    handles  => { host => 'host', },
);

has user => (
    is  => 'ro',
    isa => Str,
    predicate => 'has_user',
);

has pass => (
    is  => 'ro',
    isa => Str,
    predicate => 'has_pass',
);

=head2 new_query

This returns a new query object for you to define
by adding constraints and a view to.

=cut

sub new_query {
    require Webservice::InterMine::Query;
    my $self = shift;
    my %args = @_;

    my $roles = $args{with};

    my $query = Webservice::InterMine::Query->new(
        service => $self,
        model   => $self->model,
        %args,
    );
    return apply_roles( $query, $roles );
}

=head2 new_from_xml(source_file => $file_name)

Returns a new query by unmarshalling a serialised xml query

=cut

sub new_from_xml {
    my $self = shift;
    my %args = @_;

    my $roles = delete $args{with};

    require Webservice::InterMine::Query::Saved;
    my $query = Webservice::InterMine::Query::Saved->new(
        service => $self,
        model   => $self->model,
        %args,
    );
    return apply_roles( $query, $roles );
}

=head2 template( $name [$roles] )

This checks to see if there is a template of this name in the
webservice, and returns it to you if it exists.

=head2 get_templates() 

Returns all the templates available from the service as a list. 

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
    handles => { 
        get_template => 'get_template_by_name', 
        get_templates => 'get_templates',
    },
);

sub template {
    my ( $self, $name, %args ) = @_;
    my $t = $self->get_template($name) or return;
    return apply_roles( $t, $args{with} );
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
    handles => { 
        list => 'get_list_by_name', 
        lists => 'get_lists', 
    },
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
compatibility with different query formats. The version is always
an integer. An attempt to get the version is made on instantiation, 
which serves to validate the webservice.

=cut

has version => (
    is       => 'ro',
    isa      => ServiceVersion,
    required => 1,
    default  => sub {
        my $self = shift;
        $self->fetch( $self->root . VERSION_PATH );
    },
);

=head2 release

Returns the release string of the webservice

=cut

has release => (
    is => 'ro',
    isa => Str,
    required => 1,
    lazy => 1,
    default => sub {
        my $self = shift;
        $self->fetch( $self->root . RELEASE_PATH );
    },
);

# Returns a LWP::UserAgent suitable for getting and posting with
sub agent {
    my $self = shift;
    my $ua = LWP::UserAgent->new;
    $ua->env_proxy;
    $ua->agent(USER_AGENT);
    if (my $auth = $self->get_authstring) {
        $ua->default_header( Authorization => $auth);
    }

    return $ua;
}

sub get_authstring {
    my $self = shift;
    if ($self->has_user and $self->has_pass) {
        my $auth_string = join(':', $self->user, $self->pass);
        return encode_base64($auth_string);
    }
    return undef;
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

=head2 get_results_iterator($url, $view_list, $row_format, $json_format, $roles)

Returns a results iterator that iterates over result rows in 
the requested format. 

Parameters:

=over 4

=item $url: The path to the requested resource
=item $query_form: A hashref of query parameters
=item $view_list: an array-ref of view-paths
=item $row_format: the format for each parsed row
=item $json_format: how to handle json results
=item $roles: an optional array-ref of roles to apply

=back 

Returns a L<Webservice::InterMine::ResultIterator>

=cut

sub get_results_iterator {
    require Webservice::InterMine::ResultIterator;
    my $self        = shift;

    my $url         = shift; # The path to the resource
    my $query_form  = shift; # The query params, as a hashref
    my $view_list   = shift; # The output columns, as an arrayref
    my $row_format  = shift; # The requested row format (str)
    my $json_format = shift; # How to handle json results
    my $roles       = shift; # An optional list of roles

    my $response = Webservice::InterMine::ResultIterator->new(
        url           => $url,
        parameters    => $query_form,
        view_list     => $view_list,
        row_format    => $row_format,
        json_format   => $json_format,
        authorization => $self->get_authstring(),
    );
    confess $response->status_line, $response->content
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
    my $self = shift;
    my ( $xml, $url ) = @_;
    my $form = { xml => $xml, };
    my $resp = $self->agent->post( $url, $form );
    if ( $resp->is_error ) {
        confess $resp->status_line, "\n", $resp->content;
    } 
    return $resp->content;
}

__PACKAGE__->meta->make_immutable;
no Moose;

1;

__END__

=head1 AUTHOR

Alex Kalderimis C<dev@intermine.org>

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

