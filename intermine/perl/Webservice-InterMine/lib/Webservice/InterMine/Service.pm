package Webservice::InterMine::Service;

=head1 NAME

Webservice::InterMine::Service - an object representation of an Webservice::InterMine Web-Service

=head1 SYNOPSIS

  use Webservice::InterMine;

  my $service = Webservice::InterMine->get_service('www.flymine.org/query/service');

  # Construct queries...
  my $query    = $service->new_query(class => 'Gene');

  # Access templates...
  my $template = $service->template('Probe_Genes')
      or die "Cannot find template"

  # Manage lists
  my $new_list = $service->new_list(content => $filename, type => 'Gene');
  my $existing_list = $service->list($name);

  # Get metadata
  my $model = $service->model
 
  # Get path info
  my $path = $service->new_path("Gene.homologues.homologue.symbol");

  # ... do stuff with your objects

=head1 DESCRIPTION

The service object is the portal to the webservice: it provides the model
and templates for the objects you want to construct, and is used by the queries
to return results. Generally you won't need to interact with it directly, or
if you do, the methods you will be most interested in are those that return
objects you can use for running queries.

=cut

use Moose;
with 'Webservice::InterMine::Role::ModelOwner';
use strict;
use Net::HTTP;
use URI;
use LWP;
use MIME::Base64;
use MooseX::Types::Moose qw/Str Int/;
use Webservice::InterMine::Types
  qw(ServiceVersion ServiceRootUri TemplateFactory SavedQueryFactory ListFactory UserAgent);
use InterMine::Model::Types qw(Model);
use Carp qw(croak confess);
use Moose::Meta::Role;
use Perl6::Junction qw(any);
use Time::HiRes qw/gettimeofday/;
require Webservice::InterMine::Path;
require Webservice::InterMine::Model;

my @JSON_FORMATS = (qw/jsonobjects jsonrows jsondatatable json/);
my @SIMPLE_FORMATS = (qw/tsv tab csv count xml/);

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
    my %args;
    if      ( @_ == 3 ) {
        @args{qw/root user pass/} = @build_args;
    } elsif ( @_ == 2 and $_[0] ne "root" ) {
        @args{qw/root token/} = @build_args;
    } elsif ( @_ == 1 ) {
        @args{qw/root/} = @build_args;
    } else {
        %args = @build_args;
    }
    return $class->$orig(%args);
};

# validate the initial state of the object
sub BUILD {
    my $self = shift;
    if ($self->has_user and $self->version >= 6) {
        warnings::warnif("deprecated", "The use of username and password authentication is deprecated. Please use API token authentication");
    }
    if ($self->has_user xor $self->has_pass) {
        croak "User name or password supplied, but not both";
    }
    if ($self->has_user and $self->has_token) {
        croak "Both user/password and token credentials supplied. Please choose only one";
    }
    if ($self->has_token and ($self->version < 6)) {
        croak "This service does not support token authentication - it is only at version ", 
            $self->version;
    }
}

use constant {
    QUERY_PATH                 => '/query/results',
    QUERY_SAVE_PATH            => '/query/upload',
    QUERY_LIST_PATH            => '/query/tolist/json',
    QUERY_LIST_APPEND_PATH     => '/query/append/tolist/json',

    MODEL_PATH                 => '/model',

    TEMPLATES_PATH             => '/templates/xml',
    TEMPLATE_QUERY_PATH        => '/template/results',
    TEMPLATE_SAVE_PATH         => '/template/upload',
    TEMPLATE_LIST_PATH         => '/template/tolist/json',
    TEMPLATE_LIST_APPEND_PATH  => '/template/append/tolist/json',

    VERSION_PATH               => '/version',
    RELEASE_PATH               => '/version/release',

    LIST_PATH                  => '/lists/json',
    LISTS_WITH_OBJ_PATH        => '/listswithobject/json',

    SAVEDQUERY_PATH            => '/savedqueries/xml',

    RESOURCE_AVAILABILITY_PATH => '/check',

    POSSIBLE_VALUES_PATH       => '/path/values',

    USER_AGENT                 => 'WebserviceInterMinePerlAPIClient',
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

=head1 ATTRIBUTES

=head2 root | user | pass | token

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

for my $attr (qw/user pass token/) {
    has $attr => (is => 'ro', isa => Str, predicate => 'has_' . $attr);
}

=head2 model

The data model for the webservice. This model is required
by many modules for checking validity with the webservice
database schema. See L<InterMine::Model>

=cut

sub _build_model {
    my $self = shift;
    my $src = $self->fetch( $self->root . MODEL_PATH );
    my $model = Webservice::InterMine::Model->new(string => $src);
    $model->set_service($self);
    $model;
}

=head2 version

The version of the webservice - used for determining
compatibility with different query formats. The version is always
an integer. An attempt to get the version is made on instantiation, 
which serves to validate the webservice.

=cut

has version => (
    is       => 'ro',
    isa      => ServiceVersion,
    coerce   => 1,
    required => 1,
    default  => sub {
        my $self = shift;
        $self->fetch( $self->root . VERSION_PATH );
    },
);

=head2 release

The release string of the webservice. The release is the version
of the data-warehouse, and should change for any data added to it.

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

=head2 agent

An LWP::UserAgent suitable for getting and posting with. This agent
will authenticate to the given user and password credentials if given.

=cut

has agent => (
    is => 'ro', 
    isa => UserAgent,
    lazy_build => 1,
    handles => ['get', 'post'],
);

sub _build_agent {
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

sub build_uri {
    my $self = shift;
    my $uri = shift;
    my @params = $self->build_params(@_);
    $uri = URI->new($uri);
    $uri->query_form(@params);
    return $uri;
}

sub build_params {
    my $self = shift;
    my @params = (@_ != 1) 
        ? @_
        : (ref $_[0] eq 'HASH') ? %{$_[0]} : @{$_[0]};
    push @params, token => $self->token if ($self->has_token);
    return @params;
}


=head1 METHODS

=head2 new_query([%args])

This returns a new query object for you to define
by adding constraints and a view to.

The most useful arguments are the root class, eg:

  my $query = $service->new_query(class => 'Gene');

With this, you can use the shortcuts for adding views:

  # Adds all attributes
  $query->add_views('*');

And avoid having to repeat the root class on other calls:

 # 'Gene.' is now optional
 $query->add_constraint('symbol', '=', 'eve');

Note it is also possible to use a two parameter style for adding constraints:

 $query->add_constraint(symbol => 'eve');

See L<Webservice::InterMine::Query>

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

=head2 resultset(name) -> query

Return a new query object with the named class set as the 
root class of the query and all attribute fields
selected for output. This method is provided in part
to emulate some surface features of DBIx::Class.

  my @results = $service->resultset('Gene')->search({symbol => [qw/zen bib h eve/]});
  for my $gene (@results) {
    print $gene->name;
  }

=cut

sub resultset {
    my ($self, $name) = @_;
    my $query = $self->new_query(class => $name);
    $query->select('*');
    return $query;
}

=head2 table(name) -> query from table

Alias for resultset()

=cut

sub table { goto &resultset }

=head2 select(@views) -> query with views

Return a new query object with the given views selected for output.
This is a shortcut method for C<< $service->new_query->select(@_) >>.

  $service->select("Gene.*", "proteins.*")->where("Gene" => {in => "my-list"})->show;

=cut

sub select {
    my $self = shift;
    my @views = @_;
    my $query = $self->new_query->select(@views);
    return $query;
}

=head2 new_from_xml(source_file => $file_name)

Returns a new query by unmarshalling a serialised xml query.

See L<Webservice::InterMine::Query>

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

=head2 Template Methods:

For handling template objects, see L<Webservice::InterMine::Query::Template>

=over 2

=item * template( $name )

This checks to see if there is a template of this name in the
webservice, and returns it to you if it exists. If the user
has provided user credentials, then that user's private templates
will also be accessible. If the template exists but cannot be
parsed (and so will not run) an exception will be throws (which it up to you
to catch).

=item * get_templates() 

Returns all the templates available from the service as a list.  If 
the user has provided user credentials, then that user's private templates
will also be accessible. The number of templates returned in this list may NOT
be the same as the figure returned by C<get_template_count> - any templates that 
cannot be parsed (and so will not run) are excluded from the list.

=item * get_template_count()

Gets the count of all templates reported as available at the webservice. This may 
include broken templates.

=item * get_template_names() 

Returns a list of names of the templates available at the service. No guarantee
is made to the order these names are returned in - you may have to do some sorting 
if you require them to be alphabetical. This list includes all templates, working or
broken.

=back

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
        get_template_count => 'get_template_count',
        get_template_names => 'get_template_names',
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

=head2 List Methods

For handling list objects, see L<Webservice::InterMine::List>.

=over 4

=item list( $name )

Get the list with the given name, if it exists. Returns undef
if the given list is not accessible (either because it does not
exist or because the service is not authenticated to the correct
user account).

=item lists()

Get all the lists that this service has access to.

=item list_names()

Get all the names of the lists this service has access to.

=item new_list(content => $content, [type => $class], [name => $name], [description => $description])

Make a new list on the server with the given content and return
a L<Webservice::InterMine::List> object referring to it.

Args:

=over 2

=item * content

can be a filename, an array ref of identifiers, a string 
containing identifiers (separated by whitespace and quoted, if
necessary by double-quotes), or a L<Webservice::InterMine::Query>
(specifically, a L<Webservice::InterMine::Query::Roles::Listable>
query.

=item * type (required, unless the content is a query)

The type of the items in this list. The type must be one of the
of the classes in the model.

=item * name [optional]

The name of the list to create. If not name is supplied, one 
will be generated for you, and the resulting list will be cleaned
up (deleted) when the program exits. (Renaming the list later
will prevent this clean up, as will setting the $CLEAN_UP variable
in L<Webservice::InterMine>.)

=item * description [optional]

A long form description of the nature of this list. Optional. One will 
be generated if none is provided.

=item * path [required if the content is a Template]

The path to determine the list type for Templates.

=back

=item join_lists(\@lists, [$name, $description)

Join the given lists to create a new one (with the optional name
and description) from their union. The lists can be given as
L<Webservice::InterMine::List> objects,
L<Webservice::InterMine::Query::Roles::Listable> queries,
or the names of the lists to join.

=item subtract_lists(\@from, \@to_be_subtracted, [$name, $description])

Subtract the lists to be subtracted from the union of the
list of lists to subtract from, creating a new one 
(with the optional name and description). The lists can be given as
L<Webservice::InterMine::List> objects,
L<Webservice::InterMine::Query::Roles::Listable> queries,
or the names of lists the service has access to.

=item intersect_lists(\@lists, [$name, $description)

Intersect the given lists to create a new one (with the optional name
and description) from their intersection. The lists can be given as
L<Webservice::InterMine::List> objects,
L<Webservice::InterMine::Query::Roles::Listable> queries,
or the names of lists the service has access to.

=item diff_lists(\@lists, [$name, $description)

Calculate the symmetric difference of the given lists 
to create a new one (with the optional name
and description). The lists can be given as
L<Webservice::InterMine::List> objects,
L<Webservice::InterMine::Query::Roles::Listable> queries,
or the names of lists the service has access to.

=item delete_lists(@lists)

Delete the given lists from the service. If the lists don't exist,
an error will be thrown. Consider also calling the 
C<delete> method directly on L<Webservice::InterMine::List>
objects.

=item delete_temp_lists()

Delete all anonymous list created by the programme. This method
is automatically called on programme exist unless the variable 
C<$Webservice::InterMine::CLEAN_UP> is set to a false value.

=item get_list_data() 

Get the string containing list data from the service. 

=back

=cut

has _lists => (
    is      => 'ro',
    isa     => ListFactory,
    coerce  => 1,
    lazy_build => 1,
    writer => '_set_lists',
    handles => { 
        list => 'get_list', 
        get_list => 'get_list',
        lists => 'get_lists', 
        get_lists => 'get_lists',
        lists_with_object => 'get_lists_with_object',
        list_names => 'get_list_names',
        new_list => 'new_list',
        join_lists => 'union',
        subtract_lists => 'subtract',
        intersect_lists => 'intersect',
        diff_lists => 'symmetric_difference',
        delete_lists => 'delete_lists',
        delete_temp_lists => 'clean_up',
        list_count => 'list_count',
        refresh_lists => 'refresh_lists',
    },
);


sub _build__lists {
    my $self = shift;
    return {service => $self};
}

sub get_list_data {
    my $self = shift;
    if ($self->version < 4) {
        croak "This webservice does not support list operations";
    }
    return $self->fetch( $self->root . LIST_PATH );
}

=head2 new_path(Str path, [path => class, ...])

Construct new path objects for use with path based webservices. 
The path will be immediately validated, so it is important that any
subclass constraints that affect this path's validity are included. 
Subclass constraints can be listed as key-value pairs, or as a 
hash-ref.

EG:

  my $path = $service->new_path("Department.employees.name");
  my $path = $service->new_path("Department.employees.name", 
    "Department.employees" => "Manager");
  my $path = $service->new_path("Department.employees.name", 
    {"Department.employees" => "Manager"});

Any irrelevant subclass constraint values are ignored.

For handling path objects to retrieve lists of potential values, see
L<Webservice::InterMine::Path>

=cut

sub new_path {
    my $self = shift;
    my $path = shift;
    my $subtypes = (@_ == 1) ? shift : {@_};
    return Webservice::InterMine::Path->new($path, $self, $subtypes);
}

# Applies user supplied roles to object instances at runtime
sub apply_roles {
    my $instance = shift;
    my $roles    = shift;
    return $instance unless $roles;
    for (@$roles) {
        eval "require $_";
        confess $@ if $@;
    }
    my $combined_roles = Moose::Meta::Role->combine(map {[$_]} @$roles);
    $combined_roles->apply($instance);
    return $instance;
}

=head2 INTERNAL METHODS

=over 4

=item * get_results_iterator($url, $view_list, $row_format, $json_format, $roles)

Returns a results iterator that iterates over result rows in 
the requested format. 

Parameters:

=over 4

=item * $url: The path to the requested resource

=item * $query_form: A hashref of query parameters

=item * $view_list: an array-ref of view-paths

=item * $row_format: the format for each parsed row

=item * $json_format: how to handle json results

=item * $roles: an optional array-ref of roles to apply

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

    my $parser = $self->create_row_parser($row_format, $view_list, $json_format);
    my $request_format = $self->get_request_format($row_format);

    $query_form->{token} = $self->token if $self->has_token;

    my $response = Webservice::InterMine::ResultIterator->new(
        url           => $url,
        parameters    => $query_form,
        row_parser    => $parser,
        authorization => $self->get_authstring(),
        request_format => $request_format,
    );
    confess $response->status_line if $response->is_error;
    return apply_roles( $response, $roles );
}

sub get_request_format {
    my $self = shift;
    my $row_format = shift;
    if ($row_format eq any(@SIMPLE_FORMATS, @JSON_FORMATS)) {
        return $row_format;
    } elsif ($self->version >= 8) { # Not available earlier...
        return "json";
    } else {
        return "jsonrows";
    }
}

sub create_row_parser {
    my $self = shift;
    my ($row_format, $view_list, $json_format) = @_;
    if ($row_format eq any(@SIMPLE_FORMATS)) {
        require Webservice::InterMine::Parser::FlatFile;
        return Webservice::InterMine::Parser::FlatFile->new();
    } elsif ($row_format eq 'rr') {
        require Webservice::InterMine::Parser::JSON::ResultRows;
        return Webservice::InterMine::Parser::JSON::ResultRows->new(view => $view_list);
    } elsif ($row_format eq 'arrayrefs') {
        require Webservice::InterMine::Parser::JSON::ArrayRefs;
        return Webservice::InterMine::Parser::JSON::ArrayRefs->new();
    } elsif ($row_format eq 'hashrefs') {
        require Webservice::InterMine::Parser::JSON::HashRefs;
        return Webservice::InterMine::Parser::JSON::HashRefs->new(view => $view_list);
    } elsif ($row_format eq any(@JSON_FORMATS)) {
        require Webservice::InterMine::Parser::JSON;
        return Webservice::InterMine::Parser::JSON->new(
            json_format => $json_format, model => $self->model);
    } else {
        confess "Unknown row format '" . $row_format . "'"
    }
}

=item * fetch($url)

A simple data fetch method, for getting data as represented
by a url and doing basic error checks on the response before
returning the content. Used internally for obtaining several
items of data from the service.

=cut

sub fetch {
    my $self = shift;
    my $url  = shift;
    my $uri  = $self->build_uri($url);
    warn "FETCHING $uri " . gettimeofday() if $ENV{DEBUG};
    my $resp = $self->agent->get($uri);
    # Correct incorrect bases.
    if ($uri->host ne $resp->base->host) {
        $self->root->host($resp->base->host);
    }
    if ( $resp->is_error ) {
        confess $resp->status_line, $resp->content;
    } else {
        warn "FINISHED FETCHING $uri " . gettimeofday() if $ENV{DEBUG};
        return $resp->content;
    }
}

has resource_paths => (
    is => 'ro',
    isa => 'HashRef[Str]',
    default => sub { {} },
    traits => ['Hash'],
    handles => {
        _get_resource_path => 'get',
        put_resource_path => 'set',
    },
);

sub get_resource_path {
    my $self = shift;
    my $resource = shift;
    confess "No resource requested" unless $resource;
    if (my $path = $self->_get_resource_path($resource)) {
        return $path;
    } else {
        my $uri = URI->new($self->root . RESOURCE_AVAILABILITY_PATH . '/' . $resource);
        my $path = eval {$self->fetch($uri)};
        warn "RECEIVED $path from a request to $uri" if $ENV{DEBUG};
        confess "This webservice does not support the operation you requested: $resource" 
            unless ($path and $path =~ m|^/[a-z0-9/]+[a-z0-9]$|mi);
        $self->put_resource_path($resource, $path);
        return $path;
    }
}

=item * send_off($xml, $url)

Send xml data to the webservice, checking the response for errors.
This is used internally to save templates and queries.

=cut

sub send_off {
    my $self = shift;
    my ( $xml, $url ) = @_;
    my $uri = $self->build_uri($url);
    my $form = {xml => $xml};
    my $resp = $self->post( $url, $form );
    if ( $resp->is_error ) {
        confess $resp->status_line, "\n", $resp->content;
    } 
    return $resp->content;
}

=back

=cut

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

    perldoc Webservice::InterMine::Service

You can also look for information at:

=over 4

=item * InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://intermine.org/wiki/PerlWebServiceAPI>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.


