
=head1 NAME

Webservice::InterMine::Constraint::Logic - the logic for combining constraints on a path in a PathQuery

=head1 SYNOPSIS

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::PathQuery::Constraint

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2009 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

# This class exists to provide overloaded operators to ::Constraint and ::ConstraintSet
# as such there are no public methods, but only the operators "&" and "|"
# These allow combinations such as $query->logic($con1 & ($con2 & $con3));

package Webservice::InterMine::Role::Logical;

use MooseX::Role::WithOverloading;

use overload
  '&'      => \&_and,
  '|'      => \&_or,
  fallback => 1;

# This method overloads &
sub _and {
    return _make_node( 'and', @_ );
}

# This method overloads |
sub _or {
    return _make_node( 'or', @_ );
}

sub _make_node {
    my ( $op, $l, $r, $rev ) = @_;
    ( $l, $r ) = ( $r, $l ) if ($rev);
    require Webservice::InterMine::LogicalSet;
    return Webservice::InterMine::LogicalSet->new(
        op    => $op,
        left  => $l,
        right => $r
    );
}

1;

