
=head1 NAME

Webservice::InterMine::Constraint::LogicalSet - a pair of two object
and'ed or or'ed together

=head1 SYNOPSIS

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::Constraint::LogicalSet;

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

package Webservice::InterMine::LogicalSet;

use Moose;
use Webservice::InterMine::Role::Logical;
use Webservice::InterMine::Types qw(LogicOperator LogicGroup);
with 'Webservice::InterMine::Role::Logical';

has op => (
    is       => 'ro',
    isa      => LogicOperator,
    required => 1,
);

has [ 'left', 'right' ] => (
    is       => 'ro',
    isa      => LogicGroup,
    required => 1,
);

use overload (
    '""'     => 'code',
    fallback => 1,
);

sub code {
    my $self = shift;
    my ( $left, $right ) = map { $_->code } $self->left, $self->right;
    my $string = join(' ', $left, $self->op, $right);
    if ( $self->op eq 'or' ) {
        return "($string)";
    } else {
        return $string;
    }
}

sub constraints {
    my $self = shift;
    return map {
        ( $_->isa('Webservice::InterMine::LogicalSet') )
          ? $_->constraints
          : $_
    } $self->left, $self->right;
}

__PACKAGE__->meta->make_immutable;
no Moose;

1;
