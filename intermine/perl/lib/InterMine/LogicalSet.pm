
=head1 NAME

InterMine::Constraint::Logic::Set - an object representation of a query

=head1 SYNOPSIS

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::PathQuery::ConstraintSet

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2009 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

=head2 new

=cut

package InterMine::Constraint::LogicalSet;

use Moose;
use InterMine::Roles::Logical;
use InterMine::TypeLibrary qw(LogicOperator LogicGroup);

has op => (
	   is  => 'ro',
	   isa => LogicOperator,
	   required => 1,
);

has ['left','right'] => (
	     is  => 'ro',
	     isa => LogicGroup,
	     required => 1,
);
sub BUILD {
    my $self = shift;
    InterMine::Roles::Logical->meta->apply($self);
    return $self;
}

use overload (
    '""' => 'code',
    fallback => 1,
   );

sub code {
    my $self = shift;
    my ($left, $right) = map {$_->code} $self->left, $self->right;
    my $string = join ' '.$self->op.' ', $left, $right;
    if ($self->op eq 'or') {
	return "($string)";
    }
    else {
	return $string;
    }
}

sub constraints {
    my $self = shift;
    return map {($_->isa('InterMine::LogicalSet')) ? $_->decompose : $_} $self->left, $self->right;
}
1;
