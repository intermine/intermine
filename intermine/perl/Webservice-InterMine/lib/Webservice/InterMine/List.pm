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
union, intersection, symmetric difference, and subtraction whicj create new lists.

=cut

use Moose;
with 'Webservice::InterMine::Role::HasQuery';
with 'Webservice::InterMine::Role::Serviced';
with 'Webservice::InterMine::Role::Showable';

use Moose::Util::TypeConstraints qw(match_on_type);
use MooseX::Types::Moose         qw/ArrayRef Undef/;
use InterMine::Model::Types      qw/PathString/;
use Webservice::InterMine::Types qw/
    Date ListFactory ResultIterator Query File
    List ListableQuery ListOfLists ListOfListableQueries
    ListOperable ListOfListOperables SetObject
/;
require Set::Object;

use constant {
    LIST_APPEND_PATH => '/lists/append/json',
    RENAME_PATH => '/lists/rename/json',
    LISTABLE => 'Webservice::InterMine::Query::Roles::Listable',
    LIST => 'Webservice::InterMine::List',
};

=head1 OVERLOADED OPERATIONS

The following operations on list objects are overloaded:

=over

=item * C<""> (Stringification)

Returns a human readable representation of the list, with its name, size,
creation time, and description.

=item * C<+> (Addition)

Returns a new list from the union of this list and the other operand.
Lists can be joined to other lists, or any of the suitable content
types for new queries. In the case of lists of identifiers (whether in 
Arrays, strings or files), the type will be assumed to be the same 
as that of the current list.

=item * C<+=> (Appending)

Adds elements to the current list. Items can be appended from 
other lists, or any of the suitable content
types for new queries. In the case of lists of identifiers (whether in 
Arrays, strings or files), the type will be assumed to be the same 
as that of the current list.

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

has title => (
    is  => 'ro',
    isa => 'Str',
);

has description => (
    is => 'ro',
    isa => 'Maybe[Str]',
);

sub has_description {
    my $self = shift;
    return defined $self->description;
}

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
    my $uri = URI->new($self->service_root . RENAME_PATH);
    $uri->query_form(
        oldname => $old_name,
        newname => $name,
    );
    my $resp = $self->service->get($uri);
    my $new_list = $self->factory->parse_upload_response($resp);
    $self->clear_query;
}; 

has 'size' => (
    is  => 'ro',
    isa => 'Int',
    writer => '_set_size',
);

has 'type' => (
    is  => 'ro',
    isa => PathString,
);

has 'date' => (
    init_arg  => 'dateCreated',
    isa       => Date,
    is        => 'ro',
    coerce    => 1,
    predicate => 'has_date',
);

has '_unmatched_identifiers' => (
    is => 'ro',
    isa => 'Set::Object',
    handles => {
        add_unmatched_ids => 'insert',
        get_unmatched_ids => 'elements',
    },
    default => sub { Set::Object->new() },
);

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

sub delete {
    my $self = shift;
    return $self->factory->delete_lists($self);
}

sub overload_adding {
    my ($self, $other, $reversed) = @_;
    $self->join_with($other);
}

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

sub difference_with {
    my $self = shift;
    my $lists = $self->interpret_other_operand(shift);
    my ($name, $description) = @_;
    unshift @$lists, $self;
    return $self->factory->symmetric_difference($lists, $name, $description);
}

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

sub build_query {
    my $self = shift;
    my $q = $self->to_query;
    $q->clear_view;
    $q->add_views('*');
    return $q;
}

has element_iterator => (
    is => 'ro',
    isa => ResultIterator,
    lazy_build => 1,
    builder => 'results_iterator',
);

sub next_element {
    my $self = shift;
    my $next = $self->element_iterator->next;
    $self->clear_element_iterator unless (defined $next);
    return $next;
}

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
        ListableQuery, sub {
            my $uri = URI->new($_->get_list_append_uri);
            $uri->query_form(listName => $name, path => $path, $_->get_request_parameters);
            $resp = $self->service->get($uri);
        },
        sub {
            my $uri = URI->new($self->service_root . LIST_APPEND_PATH);
            $uri->query_form(name => $name);
            $resp = $self->service->post($uri, 'Content-Type' => $content_type, Content => $_);
        }
    );
    my $new_list = $self->factory->parse_upload_response($resp);
    $self->add_unmatched_ids($new_list->get_unmatched_ids);
    $self->_set_size($new_list->size);
    return $self;
}

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
