package InterMine::Model::Reference;

use strict;
use vars qw(@ISA);
use InterMine::Model::Field;

@ISA = qw(InterMine::Model::Field);

sub referenced_type_name
{
  return shift->{referenced_type_name};
}

sub referenced_classdescriptor
{
  my $self = shift;
  my $type_name = $self->referenced_type_name();
  return $self->{model}->get_classdescriptor_by_name($type_name);
}

sub reverse_reference_name
{
  return shift->{reverse_reference_name};
}

sub reverse_reference
{
  my $self = shift;
  my $referenced_cd = $self->referenced_classdescriptor();
  my $reverse_reference_name = $self->reverse_reference_name();
  return undef unless defined $reverse_reference_name;
  return $referenced_cd->get_field_by_name($reverse_reference_name);
}

sub has_reverse_reference
{
  my $self = shift;
  return defined $self->reverse_reference();
}

sub is_many_to_many
{
  my $self = shift;
  use Carp;
  carp if !defined $self;

  return ($self->field_type() eq 'collection' &&
          $self->has_reverse_reference() &&
          $self->reverse_reference()->field_type() eq 'collection');
}

sub is_many_to_one
{
  my $self = shift;
  return ($self->field_type() eq 'reference' &&
          defined $self->reverse_reference() &&
          $self->reverse_reference()->field_type() eq 'collection');
}

sub is_many_to_0
{
  my $self = shift;
  return ($self->field_type() eq 'collection' &&
          !defined $self->reverse_reference());
}

sub is_one_to_many
{
  my $self = shift;
  return ($self->field_type() eq 'collection' &&
          defined $self->reverse_reference() &&
          $self->reverse_reference()->field_type() eq 'reference');
}

sub is_one_to_0
{
  my $self = shift;
  return ($self->field_type() eq 'reference' &&
          !defined $self->reverse_reference());
}
