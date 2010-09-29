package InterMine::Model::Role::Descriptor;

=head1 NAME

InterMine::Model::Role::Descriptor - Provides the common behaviour of descriptors

=head1 SYNOPSIS

  use Moose;
  with 'InterMine::Model::Role::Descriptor';

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Model::Role::Descriptor

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008,2009,2010 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 Methods

=cut

use MooseX::Role::WithOverloading;
use InterMine::TypeLibrary qw(Model);
use MooseX::Types::Moose qw(Str);

use overload (
    '""' => 'to_string',
    fallback => 1,
);

=head2 to_string

The string representation of a descriptor, ie. its name

=cut 

sub to_string {
    my $self = shift;
    return $self->name;
}

has name => (
    is => 'ro',
    isa => Str,
    required => 1,
);

has model => (
    is => 'ro',
    isa => Model,
    required => 1,
);
1;
