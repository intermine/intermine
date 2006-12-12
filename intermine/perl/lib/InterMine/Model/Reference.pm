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
  return $referenced_cd->get_field_by_name($reverse_reference_name);
}

sub is_m_n_relation
{
  my $self = shift;
  return $self->field_type() eq 'collection' &&
         $self->reverse_reference()->field_type() eq 'collection';
}
