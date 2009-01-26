package InterMine::Model::Attribute;

=head1 NAME

InterMine::Model::Attribute - represents an attribute of an InterMine class

=head1 SYNOPSIS

  use InterMine::Model::Attribute;

  ...
  my $field = InterMine::Model::Attribute->new(name => 'age', model => $model,
                                               type => 'Integer');

  ...

=head1 DESCRIPTION

Objects of this class describe the attributes of class in an InterMine model.
Attribute objects are generally part of ClassDescriptor objects.

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Model::Attribute

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008,2009 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use strict;
use vars qw(@ISA);
use InterMine::Model::Field;

@ISA = qw(InterMine::Model::Field);

=head2 attribute_type

 Usage   : 

=cut

sub attribute_type
{
  my $self = shift;
  return $self->{type};
}

1;
