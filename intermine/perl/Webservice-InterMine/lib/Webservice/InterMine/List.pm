package Webservice::InterMine::List;

=head1 NAME

Webservice::InterMine::List - a representation of a List of objects
stored on a webservice's server.

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

An object of this type represents a list on the corresponding webservice's
server. While not containing the list data itself, it provides methods
for obtaining and iterating over that data, and manipulating the list
by addition, which alters the current list, and the operations of 
union, intersection, symmetric difference, and subtraction which create new lists.

Lists can be created de novo, but for that you should see L<Webservice::InterMine::Service>, which provides
the C<new_list> method.

For all active list operations (as opposed to just getting information about 
the available lists and querying against them) you will need to be authenticated to 
your user account.

=cut

use Moose;

with 'Webservice::InterMine::Role::HasQuery';
with 'Webservice::InterMine::Role::Serviced';
with 'Webservice::InterMine::Role::Showable';

use Moose::Util::TypeConstraints qw(match_on_type);
use MooseX::Types::Moose         qw/ArrayRef Undef Bool Str/;
use InterMine::Model::Types      qw/PathString/;
use Webservice::InterMine::Types qw/
    Date ListFactory ResultIterator Query File
    List Listable ListOfLists ListOfListables
    ListOperable ListOfListOperables SetObject TruthValue
/;
require Set::Object;

use constant {
    LIST_APPEND_PATH => '/lists/append/json',
    RENAME_PATH => '/lists/rename/json',
    LISTABLE => 'Webservice::InterMine::Role::Listable',
    LIST => 'Webservice::InterMine::List',
    ENRICHMENT_PATH => '/list/enrichment',
};

=head1 OVERLOADED OPERATIONS

The following operations on list objects are overloaded:

=over

=item * C<""> (Stringification)

Returns a human readable representation of the list, with its name, size,
creation time, and description.

=item * C<+>, C<|> (Unions)

Returns a new list from the union of this list and the other operand.
Lists can be joined to other lists, or any of the suitable content
types for new queries. In the case of lists of identifiers (whether in 
Arrays, strings or files), the type will be assumed to be the same 
as that of the current list.

=item * C<+=>, C<|=> (Appending)

Adds elements to the current list. Items can be appended from 
other lists, or any of the suitable content
types for new queries. In the case of lists of identifiers (whether in 
Arrays, strings or files), the type will be assumed to be the same 
as that of the current list.

=item * C<->, C<-=> (Subtraction, Removal)

Creates a new list by removing elements from the current list. 
Lists can be subtracted from other lists, as can suitable queries or
other content types, where the type will be assumed from that of the current 
list.

=item * C<&>, C<&=> (Intersections)

Creates a new list from the intersection of this list and the other operand, modifying 
the current list in the case of the assignment variant. As with other operations, where the type of an operands must 
be inferred, it will be assumed to be the same as the current list.


=item * C<^>, C<^=> (Symmetric Difference)

Creates a new list, or modifies the current one, to have the symmetric difference
of the operands - ie. only elements not shared by all operands will be in the
new list. As with other operations, where the type of an operands must 
be inferred, it will be assumed to be the same as the current list.

=item * C<< <> >> (Iteration over results)

Calling iteration over a list directly will iterate over a result
set comprising the members of the list, where the output columns are all the
attributes of the list's type. For more fine grained control over list results and 
other list queries, you can call C<to_query> to get a query of the
right class with the attribute view already added on.


=back

=cut

use overload
  '""'     => \&to_string,

    # unions: +,|
  '+'      => \&overload_adding,
  '+='     => \&overload_appending,
  '|'      => \&overload_adding,
  '|='     => \&overload_appending,

    # intersections: &
  '&'      => \&overload_intersecting,

    # Subtractions: -
  '-'      => \&overload_subtraction,
  '-='     => \&remove,

    # Symmetric differences: ^
  '^'      => \&overload_diffing,

    # Iteration: <>
  '<>'     => \&next_element,
  fallback => 1;


=head1 ATTRIBUTES

=head2 title

The permananent title of the list. This will not change over the 
list's lifetime.

=cut 

has title => (
    is  => 'ro',
    isa => 'Str',
);

=head2 description

The human readable description of the list. This can be altered, and may
be undefined.

=cut

has description => (
    is => 'ro',
    isa => 'Maybe[Str]',
);

=head2 has_description

Returns whether or not this list has a description.

=cut

sub has_description {
    my $self = shift;
    return defined $self->description;
}

=head2 name

The name of the list. This can be changed by calling C<rename>, which 
will also update the name on the server.

=cut

has name => (
    is => 'ro',
    isa => 'Str',
    trigger => \&_update_name,
    writer => 'rename',
);

sub _update_name {
    return unless (@_ > 2); 
    my ( $self, $name, $old_name ) = @_;
    return if ($name eq $old_name);
    my $uri = $self->service->build_uri($self->service_root . RENAME_PATH,
        oldname => $old_name,
        newname => $name,
    );
    my $resp = $self->service->get($uri);
    my $new_list = $self->factory->parse_upload_response($resp);
    $self->clear_query;
}; 

=head2 size

The number of members in the list. This will be updated if the list
is appended to, or if elements are removed through list 
operations.

=cut

has 'size' => (
    is  => 'ro',
    isa => 'Int',
    writer => '_set_size',
);

=head2 type

The class of object this list contains. Each list must be 
homogenous, and the classes are defined in the data-model 
for each mine. (See L<InterMine::Model>). A class defines the 
attributes and references each member possesses. The type of a 
list cannot change, unless through list operations, and then it will
only ever become less specific, and never more specific.

=cut

has 'type' => (
    is  => 'ro',
    isa => PathString,
);

=head2 date

The creation date of the list. Not all lists will necessarily have 
creation dates.

=cut

has 'date' => (
    init_arg  => 'dateCreated',
    isa       => Date,
    is        => 'ro',
    coerce    => 1,
    predicate => 'has_date',
);

=head2 status

The status of the list. Usable lists are "CURRENT", all other statuses
mean that the user should log in to the web-app to resolve issues 
caused by a data-base upgrade.

=cut

has status => (
    isa => Str,
    is  => 'ro',
    predicate => 'has_status',
);

=head2 get_unmatched_ids

Gets a list of identifiers that did not match objects when this 
list was created. This is primarily of use when debugging list creation issues. 
The set of unmatched ids does not persist accross sessions, and is only 
available immediately after a list is created.

=cut

has '_unmatched_identifiers' => (
    is => 'ro',
    isa => 'Set::Object',
    handles => {
        add_unmatched_ids => 'insert',
        get_unmatched_ids => 'elements',
    },
    default => sub { Set::Object->new() },
);

=head2 get_tags, has_tag

Get the labels this list is tagged with. Tags are short
categorisations that enable lists to be more effectively sorted and 
grouped. C<get_tags> returns the list of all tags, and C<has_tag>
returns whether the list has a particular tag.

=cut

has 'tags' => (
    is => 'ro',
    isa => SetObject,
    default => sub { Set::Object->new() },
    handles => {
        get_tags => 'elements',
        has_tag => 'contains',
    },
    coerce => 1,
);

=head2 is_authorized

Returns whether the current user has the authority to make 
changes to this list. In the case of global lists, this value will return
false. For all lists a user creates (for which you must provide authentication)
it will return true.

=cut

has 'is_authorized' => (
    isa => TruthValue,
    is => 'ro',
    default => 1,
    init_arg => 'authorized',
    coerce => 1,
);

has factory => (
    is => 'ro',
    required => 1,
    isa => ListFactory,
);


sub interpret_other_operand {
    my $self = shift;
    my $fac  = $self->factory;
    my $other = shift;
    match_on_type $other => (
        ListOperable, sub { [$_] },
        ListOfListOperables, sub { $_ },
        Undef, sub { confess "Cannot perform list operations on an undefined value"},
        sub { 
            if (ref $_ eq 'Array' and grep({$fac->get_list($_)} @$_)) {
                return $_;
            }
            return $fac->get_list($_) 
                ? [$_] 
                : [$fac->new_list(type => $self->type, content => $_)];
        }
    );
}

=head1 METHODS AND OPERATIONS

=head2 delete

Delete this list. This method deletes the list B<permanently> from the 
originating mine. B<USE WITH EXTEME CAUTION> - it B<WILL> cause loss
of data. Once this method has been called, you should not use the list 
object in any further operations, as it will throw errors.

=cut

sub delete {
    my $self = shift;
    return $self->factory->delete_lists($self);
}

sub overload_adding {
    my ($self, $other, $reversed) = @_;
    $self->join_with($other);
}

=head2 join_with(something_else)

Make a union of this list with something else. The other 
operand can be a list, a query, a string of identifiers, 
the name of a file containing identifiers, an array-ref 
of identifiers, or an array-ref of lists or queries.

=cut

sub join_with {
    my $self = shift;
    my $lists = $self->interpret_other_operand(shift);
    my ($name, $description) = @_;
    unshift @$lists, $self;
    return $self->factory->union($lists, $name, $description);
}

sub overload_intersecting {
    my ($self, $other, $reversed) = @_;
    my $intersection = $self->intersect_with($other);
    unless (defined $reversed) {
        $self->delete;
        $intersection->rename($self->name);
    }
    return $intersection;
}

=head2 intersect_with(something_else)

Make an intersection of this list with something else. The other 
operand can be a list, a query, a string of identifiers, 
the name of a file containing identifiers, an array-ref 
of identifiers, or an array-ref of lists or queries.

=cut

sub intersect_with {
    my $self = shift;
    my $lists = $self->interpret_other_operand(shift);
    my ($name, $description) = @_;
    unshift @$lists, $self;
    return $self->factory->intersect($lists, $name, $description);
}

sub overload_diffing {
    my ($self, $other, $reversed) = @_;
    my $diff = $self->difference_with($other);
    unless (defined $reversed) {
        $self->delete;
        $diff->rename($self->name);
    }
    return $diff;
}

=head2 difference_with(something_else)

Make a new list of the symmetric difference between this
list and something else. The other 
operand can be a list, a query, a string of identifiers, 
the name of a file containing identifiers, an array-ref 
of identifiers, or an array-ref of lists or queries.

=cut

sub difference_with {
    my $self = shift;
    my $lists = $self->interpret_other_operand(shift);
    my ($name, $description) = @_;
    unshift @$lists, $self;
    return $self->factory->symmetric_difference($lists, $name, $description);
}

=head2 remove(something_else)

Remove the other operand from this list. The other 
operand can be a list, a query, a string of identifiers, 
the name of a file containing identifiers, an array-ref 
of identifiers, or an array-ref of lists or queries.

=cut

sub remove {
    my ($self, $other, $reversed) = @_;
    if ($reversed) {
        return $other->remove([$self]);
    }
    my $rhs = $self->interpret_other_operand($other);
    my $new = $self->factory->subtract([$self], $rhs, 
        undef, $self->description, [$self->get_tags]);
    $self->delete;
    $new->rename($self->name);
    return $new;
}

sub overload_subtraction {
    my ($self, $other, $reversed) = @_;
    if ($reversed) {
        if (blessed $other and $other->isa(LIST)) {
            return $other->subtract($self);
        } elsif (ref $other eq 'ARRAY') {
            return $self->factory->subtract($other, [$self]);
        } else {
            confess "Both arguments to list subtraction must be lists";
        }
    } else {
        my $subtraction = $self->subtract($other);
        if (not defined $reversed) {
            $self->delete;
            $subtraction->rename($self->name);
        }
        return $subtraction;
    } 
}

=head2 subtract(something_else)

Make a new list by removing the other operand from this list. The other 
operand can be a list, a query, a string of identifiers, 
the name of a file containing identifiers, an array-ref 
of identifiers, or an array-ref of lists or queries.

=cut

sub subtract {
    my $self = shift;
    my $lists = $self->interpret_other_operand(shift);
    my ($name, $description) = @_;
    return $self->factory->subtract([$self], $lists, $name, $description);
}

sub overload_appending {
    my ($self, $other, $reversed) = @_;
    confess "Cannot append list to non-list value" if $reversed;
    $self->append($other);
}

=head2 enrichment(widget => $name, [population => $background, maxp => $val, correction => $algorithm, filter => $filter])

Receive results from an enrichment widget.

=cut

sub enrichment {
    my ($self, %options) = @_;
    my %form = %options;
    $form{correction} ||= "Holm-Bonferroni";
    $form{maxp} ||= 0.1;
    $form{list} = $self->name;
    my $uri = $self->service->build_uri($self->service_root . ENRICHMENT_PATH);
    my $iterator = $self->service->get_results_iterator($uri, \%form, [], "json", "perl", []);
    return $iterator;
}

=head2 to_query

Return a L<Webservice::InterMine::Query> representing the elements of this
list. The resulting query will have this list's type as its root class, and
already be contrained to only contain elements in this list. It will not have any
output columns. Further contraints and views can be added.

=cut

sub to_query {
    my $self = shift;
    my $query = $self->service->new_query(class => $self->type);
    $query->add_constraint($self->type, 'IN', $self);
    return $query;
}

sub get_list_upload_uri {
    my $self = shift;
    return $self->to_query->get_list_upload_uri;
}

sub get_request_parameters {
    my $self = shift;
    return $self->to_query->get_request_parameters;
}

sub get_list_request_parameters {
    my $self = shift;
    return $self->to_query->get_list_request_parameters;
}

=head2 build_query

Return a L<Webservice::InterMine::Query> representing the elements of this
list. The resulting query will have this list's type as its root class, and
already be contrained to only contain elements in this list. It will have 
all the attribute fields of the list's type as its
output columns. Further contraints and views can be added.

=cut

sub build_query {
    my $self = shift;
    my $q = $self->to_query;
    $q->clear_view;
    $q->add_views('*');
    return $q;
}

=head2 results_iterator

Get a results iterator for this list. It will by default 
iterate over results as arrayrefs, with the query set to the default values
of C<build_query>.

=cut

has element_iterator => (
    is => 'ro',
    isa => ResultIterator,
    lazy_build => 1,
    builder => 'results_iterator',
);

=head2 next_element

Get the next element (as a results row) from the list. Once the last element
is returned, the list will reset and go back to the beginning on its underlying
iterator.

=cut

sub next_element {
    my $self = shift;
    my $next = $self->element_iterator->next;
    $self->clear_element_iterator unless (defined $next);
    return $next;
}

=head2 append(something_else)

Add the elements represented by the other operand to this query. The other 
operand can be a list, a query, a string of identifiers, 
the name of a file containing identifiers, an array-ref 
of identifiers, or an array-ref of lists or queries.

=cut

sub append {
    my $self = shift;
    my $content = shift;
    my $path = shift || "";
    my $name = $self->name;
    my $resp;
    my ($ids, $content_type) = ($content, "text/plain");
    match_on_type $content => (
        List,     sub {$ids = $_->to_query},
        ListOfListOperables, sub {$ids = $self->factory->union($_)->to_query},
        ArrayRef, sub {$ids = join("\n", @$_)},
        File,     sub {$ids = [identifiers => [$content]]; $content_type = "form-data";},
        sub {}
    );
    match_on_type $ids => (
        Listable, sub {
            my $uri = $self->service->build_uri($_->get_list_append_uri,
                listName => $name, path => $path, $_->get_list_request_parameters,
            );
            $resp = $self->service->get($uri);
        },
        sub {
            my $uri = $self->service->build_uri($self->service_root . LIST_APPEND_PATH, name => $name);
            $resp = $self->service->post($uri, 'Content-Type' => $content_type, Content => $_);
        }
    );
    my $new_list = $self->factory->parse_upload_response($resp);
    $self->add_unmatched_ids($new_list->get_unmatched_ids);
    $self->_set_size($new_list->size);
    return $self;
}

=head2 add_tags(@tags)

Add the given tags to the list, updating this list on the server
and changing the tags attribute of the object.

=cut

sub add_tags {
    my $self = shift;
    my @to_add = @_;
    my @new_tags = $self->factory->add_tags($self, @to_add);
    $self->tags->clear;
    $self->tags->insert(@new_tags);
}

=head2 remove_tags(@tags)

Remove the given tags from the list, updating this list on the server
and changing the tags attribute of the object.

=cut

sub remove_tags {
    my $self = shift;
    my @to_remove = @_;
    my @new_tags = $self->factory->remove_tags($self, @to_remove);
    $self->tags->clear;
    $self->tags->insert(@new_tags);
}

=head2 update_tags()

Update the tags for this list to be up-to-date with those stored on
the server.

=cut

sub update_tags {
    my $self = shift;
    my @new_tags = $self->factory->get_tags($self);
    $self->tags->clear;
    $self->tags->insert(@new_tags);
}

=head2 to_string

Return a human readable string representation of this list. This consists of
the name, the size, the type, the creation time, and the description, if it
has all those things.

=cut

sub to_string {
    my $self = shift;
    my $ret  = sprintf( "%s (%s %ss)%s %s",
        $self->name, $self->size, $self->type,
        ( ( $self->has_date ) ? " " . $self->date->datetime : "" ),
        ( ( $self->has_description ) ? " " . $self->description : ""),
    );
    return $ret;
}


__PACKAGE__->meta->make_immutable;
no Moose;

1;

=head1 SEE ALSO

=over 4

=item * L<Webservice::InterMine::Cookbook> - A guide to using the Webservice::InterMine Perl API.

=item * L<Webservice::InterMine> - Provides the main interface to these modules.

=item * L<Webservice::InterMine::Service> - Provides factory method for all list operations.

=back

=head1 AUTHOR

Alex Kalderimis C<dev@intermine.org>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::List

You can also look for information at:

=over 4

=item * Webservice::InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/wiki/PerlWebServiceAPI>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut

