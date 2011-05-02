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
with 'Webservice::InterMine::Role::Serviced';
use InterMine::Model::Types qw(PathString );
use Webservice::InterMine::Types qw(
    Date ListFactory ResultIterator Query
    List ListableQuery ListOfLists ListOfListableQueries
);
use Moose::Util::TypeConstraints;
require Set::Object;

use constant LIST_APPEND_PATH => '/lists/append/json';
use constant RENAME_PATH => '/lists/rename/json';
use constant LISTABLE => 'Webservice::InterMine::Query::Roles::Listable';
use constant LIST => 'Webservice::InterMine::List';

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
  '+'      => \&overload_adding,
  '-'      => \&overload_subtraction,
  '+='     => \&overload_appending,
  '^'      => \&overload_intersecting,
  '|'      => \&overload_diffing,
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

has name => (
    is => 'rw',
    isa => 'Str',
    trigger => \&_update_name,
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

has factory => (
    is => 'ro',
    required => 1,
    isa => ListFactory,
);

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
    $self->intersect_with($other);
}

sub interpret_other_operand {
    my $self = shift;
    my $fac  = $self->factory;
    my $other = shift;
    match_on_type $other => (
        List, sub { [$_] },
        ListableQuery, sub { [$_] },
        ListOfLists, sub { $_ },
        ListOfListableQueries, sub { $_ },
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

sub intersect_with {
    my $self = shift;
    my $lists = $self->interpret_other_operand(shift);
    my ($name, $description) = @_;
    unshift @$lists, $self;
    return $self->factory->intersect($lists, $name, $description);
}

sub overload_diffing {
    my ($self, $other, $reversed) = @_;
    $self->difference_with($other);
}

sub difference_with {
    my $self = shift;
    my $lists = $self->interpret_other_operand(shift);
    my ($name, $description) = @_;
    unshift @$lists, $self;
    return $self->factory->symmetric_difference($lists, $name, $description);
}

sub overload_subtraction {
    my ($self, $other, $reversed) = @_;
    if ($reversed) {
        if (blessed $other and $other->isa(LIST)) {
            $other->subtract($self);
        } elsif (ref $other eq 'ARRAY') {
            $self->factory->subtract($other, [$self]);
        } else {
            confess "Both arguments to list subtraction must be lists";
        }
    } else {
        $self->subtract($other);
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

has attribute_query => (
    is => 'ro',
    isa => Query,
    lazy_build => 1,
    builder => 'get_attribute_query',
    handles => [qw/
        views view_size add_view add_views 
        results_iterator
        table_format
    /],
);

sub get_attribute_query {
    my $self = shift;
    my $q = $self->to_query;
    my @attributes = sort $q->model
                       ->get_classdescriptor_by_name($self->type)
                       ->attributes();
    $q->clear_view;
    $q->add_views(@attributes);
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
    if (blessed $content and $content->isa(__PACKAGE__)) {
        $content = $content->to_query;
    }
    if (blessed $content and $content->does(LISTABLE)) {
        my $uri = URI->new($content->get_list_append_uri);
        $uri->query_form(
            listName => $name, 
            path => $path, 
            $content->get_request_parameters,
        );
        $resp = $self->service->get($uri);
    } else {
        my $uri = URI->new($self->service_root . LIST_APPEND_PATH);
        $uri->query_form(name => $name);
        my ($identifiers, $content_type) = ($content, "text/plain");
        if (ref $content eq 'ARRAY') {
            $identifiers = join("\n", @$content);  
        } elsif (-f $content) {
            $identifiers = [identifiers => [$content]];
            $content_type = "form-data",
        }
        $resp = $self->service->post($uri,
            'Content-Type' => $content_type, 
            Content => $identifiers);
    }
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
        $self->description,
    );
    return $ret;
}

__PACKAGE__->meta->make_immutable;
no Moose;

1;
