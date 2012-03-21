package Webservice::InterMine::Query::Template;

use Moose;
use Webservice::InterMine::Query::TemplateHandler;
use Webservice::InterMine::TemplateConstraintFactory;

extends 'Webservice::InterMine::Query::Core';
with(
    'Webservice::InterMine::Query::Roles::Templated',
    'Webservice::InterMine::Query::Roles::Runnable',
    'Webservice::InterMine::Query::Roles::TemplateParameters',
    'Webservice::InterMine::Role::Serviced',
    'Webservice::InterMine::Role::Showable',
    'Webservice::InterMine::Query::Roles::ReadInAble',
    'Webservice::InterMine::Query::Roles::WriteOutAble',
    'Webservice::InterMine::Query::Roles::ExtendedQuery',
    'Webservice::InterMine::Query::Roles::Listable',
    'Webservice::InterMine::Role::Listable',
);

sub _build_handler {
    Webservice::InterMine::Query::TemplateHandler->new;
}

sub _build_constraint_factory {
    Webservice::InterMine::TemplateConstraintFactory->new;
}

__PACKAGE__->meta->make_immutable;
no Moose;

=head1 NAME

Webservice::InterMine::Query::Template - A representation of a webservice template

=head1 SYNOPSIS

  use Webservice::InterMine 'www.flymine.org/query/service';

  my $template = Webservice::InterMine->template('Probe_Genes');

  $template->get_constraint('B')->switch_off;

  my $results = $template->results_with(value1 => '1634044_at');

=head1 DESCRIPTION

Templates are shortcuts to frequently run queries. They differ from other ways
to acheive this (saved queries and scripts using the webservice) in a couple of
ways:

=over

=item * Templates can be public, and each webservice has dozens ready for you to use now

=item * They have more flexibility: some constraints are optional, others can be edited

=item * They are accessible in very few steps via the web app and the webservice api

=back

Generally they have all the same methods as queries do (see L<Webservice::InterMine::Query>),
 but they have a couple of extra methods:

=head1 METHODS

=head2 results_with( as => $format, opA => $op, valueA => $value, valueC => $value )

C<results_with> returns results in the same format as the normal query C<results> method
(which is also available to templates). This method requires the user to specify
any values and operators which are different to the template defaults.

=head2 Extra Constraint Methods:

=over

=item * editable_constraints

Returns a list of the constraint objects which represent editable constraints.
C<all_constraints> still returns all the constraint objects, some of which
may not be editable.

=item * show_constraints

returns a human readable string with information about what each
constraint does, listed by code.

=item * switch_off | switch_on

These methods are used on constraints if they are switchable.

=back

=head1 AUTHOR

Alex Kalderimis C<< <dev@intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine

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

