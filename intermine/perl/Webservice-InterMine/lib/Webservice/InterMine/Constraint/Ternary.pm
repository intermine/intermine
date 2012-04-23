package Webservice::InterMine::Constraint::Ternary;

=head1 NAME

Webservice::InterMine::Constraint::Ternary - A representation of a dual value constraint.

=head1 SYNOPSIS

    my $constraint = $query->add_constraint("Gene", "LOOKUP" "zen", "D. melanogaster");

    ok($constraint->isa("Webservice::InterMine::Constraint::Ternary"));

=head1 DESCRIPTION

Constraints are constructed by queries based on the arguments to C<add_constraint>, or a 
similar method (such as C<where>). Ternary constraints constrain the identities of objects, 
for which there is as present one operator: C<LOOKUP>.

=cut

use Moose;
extends 'Webservice::InterMine::Constraint::Binary';

use Webservice::InterMine::Types qw(TernaryOperator);
use MooseX::Types::Moose qw(Str);

has '+op' => ( isa => TernaryOperator, coerce => 1);

has 'extra_value' => (
    is  => 'ro',
    isa => Str,
);

override to_string => sub {
    my $self = shift;
    my $ev = (defined $self->extra_value) ? $self->extra_value : 'NULL';
    return join( ' ', super(), 'IN', qq{"$ev"} );
};

override to_hash => sub {
    my $self = shift;
    if ( $self->extra_value ) {    # extraValues are an optional part
        return super, ( extraValue => $self->extra_value );
    } else {
        return super;
    }
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


