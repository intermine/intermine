package InterMine::Model::Role::Descriptor;

=head1 NAME

InterMine::Model::Role::Descriptor - Provides the common behaviour of descriptors

=head1 SYNOPSIS

  use Moose;
  with 'InterMine::Model::Role::Descriptor';

=cut

use MooseX::Role::WithOverloading;
use InterMine::Model::Types qw(Model);

requires qw(to_string);

=head1 OVERLOADING

=over 4

=item STRINGIFICATION

Descriptors must implement a "to_string" method, which is 
called when string overloading occurs.

=item COMPARISON

Descriptors sort according to their (uppercased) names
 
=cut

use overload (
    '""' => 'to_string',
    fallback => 1,
);

=back 

=head1 ATTRIBUTES

=head2 model (Model, ro, required)

The model for this descriptor

=cut

has model => (
    is => 'ro',
    isa => Model,
    required => 1,
    weak_ref => 1,
);

1;

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
