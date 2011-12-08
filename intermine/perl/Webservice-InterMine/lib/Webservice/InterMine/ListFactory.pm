package Webservice::InterMine::ListFactory;

=head1 NAME

Webservice::InterMine::ListFactory - an object to manage lists and 
manipulate them

=head1 SYNOPSIS

  use Webservice::InterMine;

  my $service = Webservice::InterMine->get_service('www.flymine.org/query/service', $url, $password);

  my @lists = $service->get_lists();
  my $list = $service->list("name_of_list");
  my $new_list = $service->new_list(
    type => "Gene", content => 'indentifiers.list');
  my $intersection = $list ^ $new_list;

  while (<$intersection>) {
    printf $intersection->table_format, @$_;
  }

=head1 DESCRIPTION

The ListFactory object is a manager for lists located on the
connected webservice. It is responsible for parsing, creating,
and performing operations on list objects. Any changes made
are are reflected by changes on the server.

However, its operations are not designed to be accessed directly.
Instead call the methods available on L<InterMine::Webservice::Service>
objects, or on L<Webservice::InterMine::List> objects, both of which use
the ListFactory to provide their functionality.

=cut

use Moose;
with 'Webservice::InterMine::Role::Serviced';
with 'Webservice::InterMine::Role::KnowsJSON';

use Webservice::InterMine::List;
use Webservice::InterMine::Types qw(List ListOperable File);
use Moose::Util::TypeConstraints qw(match_on_type);
use MooseX::Types::Moose qw(HashRef Str ArrayRef);
use URI;
use Scalar::Util qw(blessed);
use List::MoreUtils qw(uniq);
require HTTP::Request::Common;
require Set::Object;
use Carp qw(croak confess);

use constant {
    LISTABLE => 'Webservice::InterMine::Query::Roles::Listable',
    DEFAULT_LIST_NAME => "my_list_",
    DEFAULT_DESCRIPTION => 'Created with Perl API client',

    UPLOAD_PATH => '/lists/json',
    DELETION_PATH => '/lists/json',
    UNION_PATH => '/lists/union/json',
    INTERSECTION_PATH => '/lists/intersect/json',
    SUBTRACTION_PATH => '/lists/subtract/json',
    DIFFERENCE_PATH => '/lists/diff/json',
    LIST_TAG_PATH => '/list/tags/json',
};


=head2 get_list( $name ) -> List

Get the list with the given name, if it exists. Returns undef
if the given list is not accessible (either because it does not
exist or because the service is not authenticated to the correct
user account).

=head2 get_lists() -> list[List]

Get all the lists that this service has access to.

=head2 list_names() -> list[Str]

Get all the names of the lists this service has access to.

=head2 new_list(content => $content, [type => $class], [name => $name], [description => $description])

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

=head2 union(\@lists, [$name, $description)

Join the given lists to create a new one (with the optional name
and description) from their union. The lists can be given as
L<Webservice::InterMine::List> objects,
L<Webservice::InterMine::Query::Roles::Listable> queries,
or the names of the lists to join.

=head2 subtract(\@from, \@to_be_subtracted, [$name, $description])

Subtract the lists to be subtracted from the union of the
list of lists to subtract from, creating a new one 
(with the optional name and description). The lists can be given as
L<Webservice::InterMine::List> objects,
L<Webservice::InterMine::Query::Roles::Listable> queries,
or the names of lists the service has access to.

=head2 intersect(\@lists, [$name, $description)

Intersect the given lists to create a new one (with the optional name
and description) from their intersection. The lists can be given as
L<Webservice::InterMine::List> objects,
L<Webservice::InterMine::Query::Roles::Listable> queries,
or the names of lists the service has access to.

=head2 symmetric_difference(\@lists, [$name, $description)

Calculate the symmetric difference of the given lists 
to create a new one (with the optional name
and description). The lists can be given as
L<Webservice::InterMine::List> objects,
L<Webservice::InterMine::Query::Roles::Listable> queries,
or the names of lists the service has access to.

=head2 delete_lists(@lists)

Delete the given lists from the service. If the lists don't exist,
an error will be thrown. Consider also calling the 
C<delete> method directly on L<Webservice::InterMine::List>
objects.

=head2 delete_temp_lists()

Delete all anonymous list created by the programme. This method
is automatically called on programme exist unless the variable 
C<$Webservice::InterMine::CLEAN_UP> is set to a false value.

=head2 list_count -> Int

Get the number of lists in the service at present

=cut

sub BUILD {
    my $self = shift;
    $self->refresh_lists;
}

has lists => (
    isa => HashRef[List],
    traits => ['Hash'],
    default => sub { {} },
    clearer => '_clear_lists',
    reader => '_lists',
    handles => {
        _set_list => 'set',
        get_list => 'get',
        get_lists => 'values',
        get_list_names => 'keys',
        list_count => 'count',
    },
);

has _temporary_list_names => (
    is => 'ro',
    isa => 'Set::Object',
    handles => {
        add_temporary_list => 'insert',
        get_temporary_lists => 'elements',
    },
    default => sub { Set::Object->new() },
);

before qr/^get_list/ => sub {
    my $self = shift;
    $self->refresh_lists;
};

sub clean_up {
    my $self = shift;
    my @lists = grep {$self->get_list($_)} $self->get_temporary_lists;
    $self->delete_lists(@lists);
}

sub _process {
    my $self = shift;
    my $str = shift;
    my $parsed = $self->decode($str);
    unless ($parsed->{wasSuccessful}) {
        confess $parsed->{error};
    }
    $self->_clear_lists;
    for my $list_info (@{$parsed->{lists}}) {
        $self->_set_list($list_info->{name}, 
            Webservice::InterMine::List->new(
                factory => $self,
                service => $self->service,
                %$list_info
            ));
    }
}

sub get_lists_with_object {
    my $self = shift;
    my $obj = shift;
    my $obj_id = eval {$obj->{objectId}} || $obj;
    my $uri = $self->service->build_uri(
        $self->service_root . $self->service->LISTS_WITH_OBJ_PATH,
        id => $obj_id,
    );
    my $str = $self->service->get($uri)->decoded_content;
    my $parsed = $self->decode($str);
    unless ($parsed->{wasSuccessful}) {
        confess $parsed->{error};
    }
    $self->refresh_lists;
    my @lists;
    for my $list_info (@{$parsed->{lists}}) {
        push @lists, $self->get_list($list_info->{name});
    }
    if (wantarray) {
        return @lists;
    } else {
        return [@lists];
    }
}

=head2 refresh_lists

Make sure the list information this object has access to is
up to date by checking for updates from the server. This method
is called when any changes are made to lists.

=cut

sub refresh_lists {
    my $self = shift;
    my $latest_data = $self->service->get_list_data;
    $self->_process($latest_data);
}

sub get_unused_list_name {
    my $self = shift;
    my $base = shift || DEFAULT_LIST_NAME;
    my $name;
    my $counter = 1;
    my $lists = $self->_lists;
    do {$name = $base . $counter++} while ($lists->{$name});
    $self->add_temporary_list($name);
    return $name;
}

sub subtract {
    my $self        = shift;
    my $references  = shift || [];
    my @ref_names   = $self->make_list_names($references);
    my $lists       = shift || [];
    my @list_names  = $self->make_list_names($lists);
    my $name        = shift || $self->get_unused_list_name;
    my $description = shift || "Subtraction of " 
            . join(' and ', @list_names) 
            . ' from ' 
            . join(' and ', @ref_names);
    my $tags = shift || [];

    my $uri = $self->service->build_uri($self->service_root . SUBTRACTION_PATH,
        name => $name,
        description => $description,
        references => join(';', @ref_names),
        subtract => join(';', @list_names),
        tags => join(';', @$tags),
    );
    my $resp = $self->service->get($uri);
    return $self->parse_upload_response($resp);
}

sub union {
    my $self = shift;
    $self->_do_commutative_list_operation(UNION_PATH, "Union", @_);
}

sub intersect {
    my $self = shift;
    $self->_do_commutative_list_operation(INTERSECTION_PATH, "Intersection", @_);
}

sub symmetric_difference {
    my $self = shift;
    $self->_do_commutative_list_operation(DIFFERENCE_PATH, "Symmetric difference", @_);
}

sub make_list_names {
    my $self = shift;
    my $lists = shift;
    my @names= eval {map {(blessed($_)) ? $_->name : $_}
            map {(blessed($_) and $_->does(LISTABLE)) 
                ? $self->new_list(content => $_) : $_} @$lists};
    confess $@ if $@;
    return @names;
}

sub _do_commutative_list_operation {
    my $self = shift;
    my $path        = shift or confess "No path given";
    my $operation   = shift or confess "No operation given";
    my $lists       = shift || [];
    my @list_names  = $self->make_list_names($lists);
    my $name        = shift || $self->get_unused_list_name;
    my $description = shift || $operation . " of " . join(' and ', @list_names);

    my $uri = $self->service->build_uri($self->service_root . $path,
        name => $name,
        lists => join(';', @list_names),
        description => $description,
    );
    my $resp = $self->service->get($uri);
    return $self->parse_upload_response($resp);
}

sub delete_lists {
    my $self = shift;
    my @list_names = uniq(map { blessed($_) ? $_->name : $_ } @_);
    for my $list (@list_names) {
        my $uri = $self->service->build_uri($self->service_root . DELETION_PATH, name => $list);
        my $resp = $self->service->agent->request(HTTP::Request::Common::DELETE($uri));
        $self->check_response_for_error($resp);
    }
    $self->refresh_lists;
}

sub new_list {
    my $self = shift;
    my %args = (@_ == 1) ? (content => shift) : @_;
    my $content = $args{content} or confess "No content passed to new_list";
    my $name        = $args{name}        || $self->get_unused_list_name;
    my $description = $args{description} || DEFAULT_DESCRIPTION;
    my $type        = $args{type}        || "";
    my $path        = $args{path}        || "";
    my $tags        = $args{tags}        || [];

    confess "Tags may not contain the ';' character" if (grep {/;/} @$tags);
    my $tag_list = join(';', @$tags);

    my $uri = $self->service->build_uri($self->service_root . UPLOAD_PATH,
        name => $name,
        description => $description, 
        type => $args{type},
        tags => $tag_list,
    );
    # Check the type, if given
    $self->service->model->get_classdescriptor_by_name($type) if $type;

    my $resp = match_on_type $content => (
        ListOperable, sub {
            $uri = $self->service->build_uri($content->get_list_upload_uri);
            my $params = {
                listName    => $name,
                description => $description,
                tags => $tag_list,
                path => $path, # needed by templates - ignored by queries 
                $content->get_list_request_parameters,
            };
            return $self->service->post($uri, $params);
        },
        ArrayRef[Str], sub {
            return $self->service->post($uri, 'Content-Type' => 'text/plain',
                Content => join("\n", map {'"' . $_ . '"'} @$content));
        },
        File, sub {
            return $self->service->post($uri, 'Content-Type' => 'form-data',
                Content => [identifiers => [$content]]);
        },
        sub {$self->service->post($uri, 'Content-Type' => 'text/plain', Content => $content)}
    );

    return $self->parse_upload_response($resp);
}

sub add_tags {
    my ($self, $list, @tags) = @_;
    my %params = (
        name => $list->name,
        tags => join(";", @tags),
    );
    my @params = $self->service->build_params(%params);
    my $uri = $self->service->root . LIST_TAG_PATH;
    my $response = $self->service->post($uri, \@params);
    $self->check_response_for_error($response);
    my $data = $self->decode($response->content);
    return @{ $data->{tags} };
}

sub remove_tags {
    my ($self, $list, @tags) = @_;
    my %params = (
        name => $list->name,
        tags => join(";", @tags),
    );
    my $uri = $self->service->build_uri(
        $self->service_root . LIST_TAG_PATH, %params);
    my $resp = $self->service->agent->request(
        HTTP::Request::Common::DELETE($uri));
    $self->check_response_for_error($resp);
    my $data = $self->decode($resp->content);
    return @{ $data->{tags} };
}

sub get_tags {
    my ($self, $list) = @_;
    my %params = (
        name => $list->name,
    );
    my $uri = $self->service->build_uri(
        $self->service_root . LIST_TAG_PATH, %params);
    my $resp = $self->service->get($uri);
    $self->check_response_for_error($resp);
    my $data = $self->decode($resp->content);
    return @{ $data->{tags} };
}

sub check_response_for_error {
    my ($self, $resp) = @_;
    my $json;
    if ($resp->is_error) {
        my $error = eval {
            $json = $self->decode($resp->content);
            return $json->{error};
        } || $resp->status_line . $resp->content;
        confess $error;
    }
}

sub parse_upload_response {
    my $self = shift;
    my $resp = shift;
    $self->check_response_for_error($resp);
    my $json = $self->decode($resp->content);
    my $created = $json->{listName};
    my $unmatched_identifiers = $json->{unmatchedIdentifiers} || [];
    $self->refresh_lists;
    my $ret = $self->get_list($created);
    $ret->add_unmatched_ids(@$unmatched_identifiers);
    return $ret;
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

    perldoc Webservice::InterMine::ListFactory

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
