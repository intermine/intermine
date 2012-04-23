package Webservice::InterMine::Role::HasQuery;

=head1 NAME

Webservice::InterMine::Role::HasQuery 

=head1 SYNOPSIS

Common behaviour for objects that can represent themselves as queries.

  use Webservice::InterMine;

  my $service = Webservice::InterMine->get_service('www.flymine.org/query/service', $token);

  # Lists are consumers of HasQuery
  my $list = $service->list("my_genes");

  my $sub_list = $service->new_list(
    name => "genes in my_genes with a certain domain",
    content => $list->where("proteins.proteinDomain.name" => "Some-Protein-Domain"));

  for my $row ($sub_list->select("symbol")->all()) {
    say @$row;
  }

=head1 DESCRIPTION

A consumer of this role gains many of the capabilities of a query, such as reading
results, iteration, introspection of views and constraints. 

=cut


use Moose::Role;

use Webservice::InterMine::Types qw(Query);

requires 'build_query';

=head2 view

Return the list of columns in the view.

=cut

sub view {};

=head2 view_size 

Return the size of the view list.

=cut

sub view_size {};

sub add_view {};
sub add_views {};
sub results_iterator {};
sub results {};
sub all {};

=head2 select(@columns)

Return a new query with the given columns selected for output.

=cut

sub select {
    my ($self, @views) = @_;
    my $query = $self->build_query();
    return $query->select(@views);
}

=head2 where(@constraint_args)

Return a new query with the given constraints applied.

=cut

sub where {
    my $self = shift;
    return $self->build_query()->where(@_);
}

has query => (
    is => 'ro',
    isa => Query,
    lazy_build => 1,
    builder => 'build_query',
    handles => [qw/
        views view_size add_view add_views 
        results_iterator results all
    /],
);

1;

__END__

=head1 SEE ALSO

=over 4

=item * L<Webservice::InterMine::Cookbook> - A guide to using the Webservice::InterMine Perl API.

=item * L<Webservice::InterMine> - Provides the main interface to these modules.

=item * L<Webservice::InterMine::List> - A consumer of HasQuery

=back

=head1 AUTHOR

Alex Kalderimis C<dev@intermine.org>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::Role::HasQuery

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
