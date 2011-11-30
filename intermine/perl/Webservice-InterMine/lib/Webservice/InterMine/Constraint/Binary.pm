package Webservice::InterMine::Constraint::Binary;

=head1 NAME

Webservice::InterMine::Constraint::Binary - A representation of a binary attribute constraint

=head1 SYNOPSIS

    my $constraint = $query->add_constraint("symbol", "=", "zen");

    ok($constraint->isa("Webservice::InterMine::Constraint::Binary"));

=head1 DESCRIPTION

Constraints are constructed by queries based on the arguments to C<add_constraint>, or a 
similar method (such as C<where>). Binary constraints constrain the values of attributes, and can 
have one of the following operators:

=over

=item * C<< = >>

Equality. Allows wildcards with C<*> (such as C<symbol = zen*>). All equality 
operations are case insensitive, so C<symbol = zen> and C<symbol = ZEN> are 
identical.

=item * C<< != >>

The inverse of C<< = >>.

=item * C<< < >>, C<< <= >>, C<< > >>, C<< >= >>

Numeric comparison. For strings, case sensitive alphanumeric sorting will be performed.

=back

=cut

use Moose;

extends 'Webservice::InterMine::Constraint';
with 'Webservice::InterMine::Constraint::Role::Operator';
use Webservice::InterMine::Types qw(BinaryOperator);
use MooseX::Types::Moose qw(Str);

has '+op' => ( isa => BinaryOperator, coerce => 1);

=head1 ATTIBUTES

=head2 value

the value this constraint constrains its attribute in relation to.

=cut

has 'value' => (
    is       => 'ro',
    isa      => Str,
    required => 1,
    writer   => 'set_value',
);

has 'extra_value' => (
    is       => 'ro',
    isa      => Str,
);

override to_string => sub {
    my $self = shift;
    return join( ' ', super(), $self->op, '"' . $self->value . '"' );
};

override to_hash => sub {
    my $self = shift;
    return ( super, $self->operator_hash_bits, value => $self->value );
};

__PACKAGE__->meta->make_immutable;
no Moose;
1;


__END__

=head1 AUTHOR

Alex Kalderimis C<dev@intermine.org>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this distribution with the perldoc command.

    perldoc Webservice::InterMine

You can also look for information at:

=over 4

=item * InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://intermine.org/wiki/PerlWebServiceAPI>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.


