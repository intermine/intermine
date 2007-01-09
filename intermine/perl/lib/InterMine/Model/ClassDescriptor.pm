package InterMine::Model::ClassDescriptor;

use strict;

sub new
{
  my $class = shift;
  my %opts = @_;
  my $self = {%opts};
  $self->{fields} = [];
  $self->{field_hash} = {};

  $self->{extends} = $opts{extends};

  if (!exists $opts{name}) {
    die "no name given to class constructor\n";
  }
  if (!exists $opts{model}) {
    die "no model given to class constructor\n";
  }

  bless $self, $class;
  return $self;
}

sub add_field
{
  my $self = shift;
  my $field = shift;

  return if defined $self->get_field_by_name($field->field_name());

  push @{$self->{fields}}, $field;

  if (!exists $self->{field_hash}{$field->field_name}) {
    $self->{field_hash}{$field->field_name()} = $field;
  }

  if (ref $field eq 'InterMine::Model::Attribute') {
    push @{$self->{attributes}}, $field;
  } else {
    if (ref $field eq 'InterMine::Model::Reference') {
      push @{$self->{references}}, $field;
    } else {
      if (ref $field eq 'InterMine::Model::Collection') {
        push @{$self->{collections}}, $field;
      } else {
        die "unknown reference: ", $field, "\n";
      }
    }
  }
}

sub name
{
  my $self = shift;
  return $self->{name};
}

sub unqualified_name
{
  my $self = shift;
  return ($self->{name} =~ /.*\.(.*)/)[0];
}

sub extends
{
  my $self = shift;
  return @{$self->{extends}};
}

sub extends_class_descriptors
{
  my $self = shift;

  if (!defined $self->{extends_class_descriptors}) {
    $self->{extends_class_descriptors} = [];
    for my $extendee (@{$self->{extends}}) {
      my $extendee_cd = $self->{model}->get_classdescriptor_by_name($extendee);

      if (defined $extendee_cd) {
        push @{$self->{extends_class_descriptors}}, $extendee_cd;
      } else {
        die "can't find $extendee in the model\n"
      }
    }
  }
  return @{$self->{extends_class_descriptors}};
}

sub get_field_by_name
{
  my $self = shift;
  my $field = shift;

  return $self->{field_hash}{$field};
}

sub valid_field
{
  my $self = shift;
  my $field = shift;
  return defined $self->get_field_by_name($field);
}

sub attributes
{
  my $self = shift;
  return @{$self->{attributes}}
}

sub fields
{
  my $self = shift;
  return @{$self->{fields}}
}

sub references
{
  my $self = shift;
  return @{$self->{references}}
}

sub collections
{
  my $self = shift;
  return @{$self->{collections}}
}

sub sub_class_of
{
  my $self = shift;
  my $other_class_desc = shift;

  if ($self == $other_class_desc) {
    return 1;
  } else {
    for my $extendee_class_desc ($self->extends_class_descriptors()) {
      if ($extendee_class_desc->sub_class_of($other_class_desc)) {
        return 1;
      }
    }
  }

  return 0;
}

1;
