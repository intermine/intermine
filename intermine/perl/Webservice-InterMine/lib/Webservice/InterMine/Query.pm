package Webservice::InterMine::Query;

use Moose;
use Webservice::InterMine::ConstraintFactory;

extends 'Webservice::InterMine::Query::Core';
with(
    'Webservice::InterMine::Query::Roles::Runnable',
    'Webservice::InterMine::Query::Roles::QueryParameters',
    'Webservice::InterMine::Query::Roles::WriteOutAble',
    'Webservice::InterMine::Query::Roles::WriteOutLegacy',
    'Webservice::InterMine::Query::Roles::Templateable',
    'Webservice::InterMine::Query::Roles::Listable',
    'Webservice::InterMine::Role::Listable',
    'Webservice::InterMine::Role::Serviced',
    'Webservice::InterMine::Role::Showable',
);

__PACKAGE__->meta->make_immutable;
no Moose;
1;

=head1 NAME

Webservice::InterMine::Query - an object representation of a query on
an InterMine database

=head1 SYNOPSIS

  # Queries should be created by their webservices, or through
  # the Webservice::InterMine factory module

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
See L<Webservice::InterMine::Cookbook::Recipe2> and L<Webservice::InterMine::Cookbook::Recipe3>.

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

Adds a join description to the query (see L<Webservice::InterMine::Cookbook::Recipe4>).

=head2 add_pathdescription(%args)

Adds a path description to the query (see L<Webservice::InterMine::Cookbook::Recipe4>).

=head2 logic

Get the logic for the query as an object. The Logic object is string overloaded to
its human readable string representation (also available with ->code). 

see: L<Webservice::InterMine::LogicalSet>

=head2 set_logic(EXPR or $str)

Sets the current logic for the query. Illegal logic
expressions or strings will cause exceptions to be thrown.

Examples:

  # Parse a string
  $query->set_logic("A and (B or C)");

  # Perform boolean logic on constraint objects
  $query->set_logic($conA & ($conB | $conC));

=head2 results([as => Str, size => Int, start => Int, columnheaders => Bool])

Gets a page of results (defined by start and size - defaulting
to all results from the beginning) in a requested format. (See
L<Webservice::InterMine::Cookbook::Recipe5>) 

The formats are:

=over 4

=item * string

Returns all rows as one string, with fields separated by tabs and lines
separated by new-lines ("\n"). If you are wanting to simply store
the results in a flat file, this is probably what you want.

If you would like column headers, add the parameter: 
<code>columnheaders => 1</code>

=item * strings

Returns all rows as strings (without new-lines) in an arrayref.

=item * hashref[s] [default]

Returns an arrayref of hashrefs, where the keys are the view columns.

=item * arrayref[s]

Returns an arrayref of arrayrefs, where the fields are in the same
order as the view columns.

=item * count 

Returns the total number or results, rather than the results themselves.

=item * jsonobjects

By default returns an arrayref of native perl data structures (hashrefs) which 
correspond to the data format of InterMine jsonobjects 
(L<http://www.intermine.org/wiki/JSONObjectFormat>). 

Optionally it can return the jsonobjects processed as:

=over 8

=item * raw: the raw text string

=item * inflate: inflated objects using autoload

The inflated objects allow method access to the internal hash using
autoloaded accessors, but all objects will be blessed into the same type
(L<Webservice::InterMine::ResultObject>), so there is no guarantee of 
interface (other than inspecting the internal hash).

=item * instantiate: real Moose objects

The instantiated objects allow method access to their internal hash using
true accessors, with type-constraints, inheritance, support for "isa" and 
"ref", coercion of attributes, delegating collections, 
and all the other things you would expect from
a Moose object. Each object will be instantiated as an instance of its
class (ie. a $gene will be a member of the "Gene" class).

=back

=back

=head2 results_iterator

returns a results object that allows you to iterate through the
results row by row, in whatever format you choose. Please see
L<Webservice::InterMine::Cookbook::Recipe6> and L<Webservice::InterMine::Cookbook::Recipe7>.

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

=item * L<Webservice::InterMine::Cookbook> - A guide to using the Webservice::InterMine Perl API

=item * L<Webservice::InterMine>

=item * L<Webservice::InterMine::Service>

=back

=head1 AUTHOR

Alex Kalderimis C<dev@intermine.org>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::Query

You can also look for information at:

=over 4

=item * Webservice::InterMine

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

