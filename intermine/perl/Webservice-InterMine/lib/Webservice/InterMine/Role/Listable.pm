package Webservice::InterMine::Role::Listable;

=head1 NAME

Webservice::InterMine::Role::Listable - Trait for things that can be made into lists

=head1 SYNOPSIS

  my $query = get_service->select("Gene.*");
  my $list = $service->new_list(content => $query);

=head1 DESCRIPTION

This role provides the common behaviour for things that can be passed to the 
new_list method of service objects, and automatically coerced into lists, as well as treated
as lists for the purposes of list operations.

=cut

use MooseX::Role::WithOverloading;

=head1 REQUIREMENTS

Classes that consume this role must provide implementations of the following methods:

=over 4

=item * service_root: The root url of the service.

=item * list_upload_path: The path to append to the service root when creating a new list.

=item * to_query: The query to be run to generate a list.

=item * get_request_parameters: The parameters to be passed to the service.

=item * service: The service object

=item * list_append_path: The path to append to the service root when adding items.

=back

=cut

requires 'list_upload_path', 'service_root', 'get_list_request_parameters', 'service', 'list_append_path';

=head1 OVERLOADING

This role provides the following overloaded operations. 

=head2 C<|>,C<+>: Unions

  $a | $b

create a union of two Listables

=head2 C<&>: Intersections

  $a & $b

Create a list of the intersection of two listables.

=head2 C<^>: Symmetric Difference

  $a ^ $b

Create a list from the symmetric difference of two listables (the inverse of their intersection).

=head2 C<->: Asymmetric Difference (subtraction)

  $a - $b

Create a list of all the elements in C<$a> less the elements in C<$b>.

=cut

use overload 
   '|' => \&_overload_adding,
   '+' => \&_overload_adding,
   '&' => \&_overload_intersecting,
   '^' => \&_overload_diffing,
   '-' => \&_overload_subtraction,
   fallback => 1;

=head1 METHODS

This role provides the following functions:

=head2 get_list_upload_uri

Get a uri to upload a list to.

=cut

sub get_list_upload_uri {
    my $self = shift;
    return $self->service_root . $self->list_upload_path;
}

=head2 get_list_append_uri

Get a uri to post to when appending items to the list.

=cut

sub get_list_append_uri {
    my $self = shift;
    return $self->service_root . $self->list_append_path;
}

sub _overload_adding {
    my ($self, $other, $reversed) = @_;
    return $self->service->join_lists([$self, $other]);
}

sub _overload_intersecting {
    my ($self, $other, $reversed) = @_;
    return $self->service->intersect_lists([$self, $other]);
}

sub _overload_diffing {
    my ($self, $other, $reversed) = @_;
    return $self->service->diff_lists([$self, $other]);
}

sub _overload_subtraction {
    my ($self, $other, $reversed) = @_;
    if ($reversed) {
        return $self->service->subtract_lists([$other], [$self]);
    } else {
        return $self->service->subtract_lists([$self], [$other]);
    }
}

1;
