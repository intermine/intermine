package InterMine::Model::ClassDescriptor;

use strict;

sub new
{
  my $class = shift;
  my %opts = @_;
  my $self = {%opts};
  $self->{fields} = [];
  $self->{field_hash} = {};

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

1;
