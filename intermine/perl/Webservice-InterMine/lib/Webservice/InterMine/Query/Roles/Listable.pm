package Webservice::InterMine::Query::Roles::Listable;

=head1 NAME

Webservice::InterMine::Query::Roles::Listable - Trait for queries that are Listable

=head1 SYNOPSIS

  my $query = get_service->select("Gene.*");
  my $list = $service->new_list(content => $query);

=head1 DESCRIPTION

This role provides an implementation of the required C<get_list_request_parameters> method of the 
more general Listable role for queries.

=cut

use Moose::Role;

requires "clone";

=head2 get_list_request_parameters

Get the parameters to pass to the service for list requests.

=cut

sub get_list_request_parameters {
    my $self = shift;
    my $clone = $self->clone;
    my @views = $clone->views;
    if (@views == 0 && $clone->has_root_path) {
        $clone->select("id");
    } elsif (@views > 1 || $views[0] !~ /\.id$/) {
        my %froms = map {$clone->path($_)->prefix() => 1} @views;
        my @froms = keys %froms;
        if (@froms > 1) {
            confess "Cannot generate a valid list request - more than one class is selected";
        } else {
            $clone->select($froms[0] . ".id");
        }
    } 
        
    my %params = $clone->get_request_parameters;
    return %params;
}
1;
