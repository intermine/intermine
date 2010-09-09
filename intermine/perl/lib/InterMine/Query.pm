package InterMine::Query;

use Moose;
use InterMine::ConstraintFactory;

extends 'InterMine::Query::Core';
with qw(
	   InterMine::Query::Roles::Runnable
	   InterMine::Query::Roles::WriteOutAble
	   InterMine::Query::Roles::Serviced
      );

__PACKAGE__->meta->make_immutable;
no Moose;
1;

=head1 NAME

InterMine::Query - an object representation of a query on
an InterMine database

=head1 SYNOPSIS

  # Queries should be created by their webservices, or through
  # the InterMine factory module

  my $query = $service->new_query;

  $query->name('Genes_Annotated_Biological_Process');
  $query->description(
      'Get all genes in Drosophilids annotated with GO Terms in the Biological Process namespace'
  );

  $query->add_view(
      'Gene.name',
      'Gene.primaryIdentifier',
      'Gene.goAnnotation.ontologyTerm.name',
  );

  $query->set_sort_order('Gene.primaryIdentifier', 'asc');

  $query->add_constraint(
      path  => 'Gene.goAnnotation.ontologyTerm.namespace',
      op    => '=',
      value => 'biological_process',
  );

  $query->add_constraint(
      path  => 'Gene.organism.name',
      op    => 'CONTAINS',
      value => 'Drosophila',
  );

  my $results = $query->results(as => 'arrayrefs');

  for my $row (@$results) {
      my ($name, $id, $go_term) = @$row;
      print "$name ($id): $go_term\n";
  }

=head1 DESCRIPTION

This module is allows you to construct complex queries and obtain
results from webservices which implement InterMine genomic databases.

=head1 METHODS

=head2 add_view( [@paths] | [$comma_sep_paths] | [$space_sep_paths])

Adds the paths to the view (result columns). If the argument to C<add_view>
is a comma or space separated string, it will be split automatically. All
paths must be valid for the model being used, or an exception will be thrown.

=head2 set_sort_order( $path, [$direction] )

Defines the sort order for the results to be a particular column in the view
(this column must already be in the view, or an exception will be thrown).
You do not have to set the sort order manually, the default column is the
first column of the view, and the default direction is 'asc'.

=head2 constraint methods:

=over

=item * add_constraint(%args or $string)

Adds a constraint the the query. Returns the constraint object.
See L<InterMine::Cookbook::Recipe2> and L<InterMine::Cookbook::Recipe3>.

=item * all_constraints

Get a list of all the constraint objects attached to the query.

=item * remove_constraint($constraint_obj)

Removes the constraint from the query

=item * find_constraints( \&coderef )

Finds all constraints that match the code reference criterion - eg:

  my @eq_cons = $query->find_constraints(sub {$_->op eq '='});

=item * count_constraints

Returns the number of constraints on the query (or 0 if none)

=item * clear_constraints

Removes all the constraints from the query

=item * coded_constraints

Returns only the constraints that have codes (and can thus be
combined to define the logic)

=item * constraint_codes

Returns the contraint codes currently being used by the constraints
on the query.

=back

=head2 add_join(%args or $path)

Adds a join description to the query (see L<InterMine::Cookbook::Recipe4>).

=head2 add_pathdescription(%args)

Adds a path description to the query (see L<InterMine::Cookbook::Recipe4>).

=head2 logic([EXPR or $str])

Gets or sets the current logic for the query as an object (calling C<code>
on the logic object gets a human readable string version). Illegal logic
expressions or strings will cause exceptions to be thrown.

=head2 results([as => $format])

Gets the results for this query in a variety of formats. (see
L<InterMine::Cookbook::Recipe5>) The four default
formats are:

=over 4

=item * string

Returns all rows as one string, with fields separated by tabs and lines
separated by new-lines ("\n"). If you are wanting to simply store
the results in a flat file, this is probably what you want.

=item * strings

Returns all rows as strings (without new-lines) in an arrayref.

=item * hashref[s] [default]

Returns an arrayref of hashrefs, where the keys are the view columns.

=item * arrayref[s]

Returns an arrayref of arrayrefs, where the fields are in the same
order as the view columns.

=back

=head2 results_iterator

returns a results object that allows you to iterate through the
results row by row, in whatever format you choose. Please see
L<InterMine::Cookbook::Recipe6> and L<InterMine::Cookbook::Recipe7>.

=head2 suspend_validation | resume_validation | validate

Queries will by default check for errors in construction as they are
being defined. You can choose to turn this off, which might be useful
if you don't want to care about the order you add elements to the
query (ie. you might call C<set_sort_order> before C<add_view>), or if
you are concerned about speed. In most cases the recommendation is
to B<not turn validation off>, unless of course, you really know what
you are doing. Calling C<validate> at any point will always perform
the validation checks, even if you have suspended validation.

=head2 to_xml

Returns an xml string representation of the query. This is suitable for
serialisation of queries.

=head1 SEE ALSO

=over 4

=item * L<InterMine::Cookbook> - A guide to using the InterMine Perl API

=item * L<InterMine>

=item * L<InterMine::Service>

=back

=head1 AUTHOR

Alex Kalderimis C<dev@intermine.org>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Query

You can also look for information at:

=over 4

=item * InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/perlapi>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2010 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut

1;


