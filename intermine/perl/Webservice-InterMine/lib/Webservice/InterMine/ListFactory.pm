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
use Webservice::InterMine::Types qw(List);
use MooseX::Types::Moose qw(HashRef Str);
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
};

sub BUILD {
    my $self = shift;
    $self->refresh_lists;
}

has lists => (
    isa => HashRef[List],
    traits => ['Hash'],
    default => sub { {} },
    clearer => '_clear_lists',
    handles => {
        _set_list => 'set',
        get_list => 'get',
        get_lists => 'values',
        get_list_names => 'keys',
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
    do {$name = $base . $counter++} while ($self->get_list($name));
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

    my $uri = URI->new($self->service_root . SUBTRACTION_PATH);
    $uri->query_form(
        name => $name,
        description => $description,
        references => join(';', @ref_names),
        subtract => join(';', @list_names),
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

    my $uri = URI->new($self->service_root . $path);
    $uri->query_form(
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
        my $uri = URI->new($self->service_root . DELETION_PATH);
        $uri->query_form(name => $list);
        my $resp = $self->service->agent->request(
            HTTP::Request::Common::DELETE($uri));
        $self->check_response_for_error($resp);
    }
    $self->refresh_lists;
}

sub new_list {
    my $self = shift;
    my %args = @_;
    my $content = $args{content} or confess "No content passed to new_list";
    my $name        = $args{name}        || $self->get_unused_list_name;
    my $description = $args{description} || DEFAULT_DESCRIPTION;
    my $type        = $args{type}        || "";
    my $path        = $args{path}        || "";

    if (blessed $content and $content->does(LISTABLE)) {
        my $uri = URI->new($content->get_list_upload_uri);
        $uri->query_form(
            listName    => $name,
            description => $description,
            path => $path, # needed by templates - ignored by queries 
            $content->get_request_parameters,
        );
        my $resp = $self->service->agent->get($uri);
        return $self->parse_upload_response($resp);
    }

    if ($type) {
        $self->service->model->get_classdescriptor_by_name($type);
    } else {
        confess "No type information supplied";
    }

    my $uri = URI->new($self->service_root . UPLOAD_PATH);
    $uri->query_form(
        name => $name,
        description => $description, 
        type => $args{type},
    );
    if (ref $content eq 'ARRAY') {
        my $resp = $self->service->agent->post($uri, 
            'Content-Type' => 'text/plain',
            Content => join("\n", map {'"' . $_ . '"'} @$content));
        return $self->parse_upload_response($resp);
    }
    if (-f $content) {
        my $resp = $self->service->agent->post($uri,
            'Content-Type' => 'form-data',
            Content => [identifiers => [$content]]);
        return $self->parse_upload_response($resp);
    }
    my $resp = $self->service->agent->post($uri, 
        'Content-Type' => 'text/plain',
        Content => $content);
    return $self->parse_upload_response($resp);
}

sub check_response_for_error {
    my ($self, $resp) = @_;
    if ($resp->is_error) {
        my $error = eval {
            my $json = $self->decode($resp->content);
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
