package InterMine::Model::Reference;

=head1 NAME

InterMine::Model::Reference - represents a reference in an InterMine class

=head1 SYNOPSIS

 use InterMine::Model::Reference
 ...
 my $field = InterMine::Model::Reference->new(name => 'protein',
                                              model => $model,
                                              referenced_type_name => $ref_type,
                                              reverse_reference_name =>
                                                         $reverse_reference);
 ...

=head1 DESCRIPTION

Objects of this class describe the references and collections of a class
in an InterMine model.  Reference objects are generally part of
ClassDescriptor objects.

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Model::Reference

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

=head2 referenced_type_name

 Usage   : $name = $ref->referenced_type_name();
 Function: Returns the name of the type at the other end of this reference
 Args    : none

=cut
sub referenced_type_name
{
  my $name = shift->{referenced_type_name};
  return $name;
}

=head2 referenced_classdescriptor

 Usage   : $cd = $ref->referenced_classdescriptor();
 Function: Returns the ClassDescriptor of the other end of this reference
 Args    : none

=cut
sub referenced_classdescriptor
{
  my $self = shift;
  my $type_name = $self->referenced_type_name();
  return $self->{model}->get_classdescriptor_by_name($type_name);
}

=head2 referenced_classdescriptor

 Usage   : $cd = $ref->rev_referenced_classdescriptor();
 Function: Returns the ClassDescriptor of the other end of this reference
 Args    : none

=cut
sub rev_referenced_classdescriptor {
    my $self       = shift;
    my $field_name = $self->field_name();
    return $self->{model}->get_referenced_classdescriptor($field_name);
}

=head2 reverse_reference_name

 Usage   : $name = $ref->reverse_reference_name();
 Function: Return the name of the reverse reference - ie. the name of the field
           in the referenced class that references this class
 Args    : none

=cut
sub reverse_reference_name
{
  return shift->{reverse_reference_name};
}

=head2 reverse_reference

 Usage   : $ref = $ref->reverse_reference();
 Function: Return the Reference object of the reverse reference
 Args    : none

=cut
sub reverse_reference
{
  my $self = shift;
  my $referenced_cd = $self->referenced_classdescriptor();
  my $reverse_reference_name = $self->reverse_reference_name();
  return undef unless defined $reverse_reference_name;
  return $referenced_cd->get_field_by_name($reverse_reference_name);
}

=head2 has_reverse_reference

 Usage   : if ($ref->has_reverse_reference()) { ... }
 Function: Return true if and only if this reference has a reverse reference in
           the model
 Args    : none

=cut
sub has_reverse_reference
{
  my $self = shift;
  return defined $self->reverse_reference();
}

=head2 is_many_to_many

 Function: Return true if this reference is one end of a many-to-many relation,
           ie. this end is a collection and the other end is a collection
 Args    : none

=cut
sub is_many_to_many
{
  my $self = shift;
  use Carp;
  carp if !defined $self;

  return ($self->field_type() eq 'collection' &&
          $self->has_reverse_reference() &&
          $self->reverse_reference()->field_type() eq 'collection');
}

=head2 is_many_to_one

 Function: Return true if this is the reference end of a one-to-many relation,
           ie. this end is a reference and the other end is a collection
 Args    : none

=cut
sub is_many_to_one
{
  my $self = shift;
  return ($self->field_type() eq 'reference' &&
          defined $self->reverse_reference() &&
          $self->reverse_reference()->field_type() eq 'collection');
}

=head2 is_many_to_0

 Function: Return true if this is a collection and there is no reverse reference
 Args    : none

=cut
sub is_many_to_0
{
  my $self = shift;
  return ($self->field_type() eq 'collection' &&
          !defined $self->reverse_reference());
}

=head2 is_one_to_many

 Function: Return true if this is the collection end of a one-to-many relation,
           ie. this end is a collection and the other end is a reference
 Args    : none

=cut
sub is_one_to_many
{
  my $self = shift;
  return ($self->field_type() eq 'collection' &&
          defined $self->reverse_reference() &&
          $self->reverse_reference()->field_type() eq 'reference');
}

=head2 is_one_to_0

 Function: Return true if this is a reference and the is no reverse reference

=cut
sub is_one_to_0
{
  my $self = shift;
  return ($self->field_type() eq 'reference' &&
          !defined $self->reverse_reference());
}
