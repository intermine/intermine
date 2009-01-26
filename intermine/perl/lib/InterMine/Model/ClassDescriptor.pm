package InterMine::Model::ClassDescriptor;

=head1 NAME

InterMine::Model::ClassDescriptor - represents a class in an InterMine model

=head1 SYNOPSIS

 use InterMine::Model::ClassDescriptor;

 ...
 my $cd = 
   new InterMine::Model::ClassDescriptor(model => $model,
                                         name => "Gene", extends => ["BioEntity"]);

=head1 DESCRIPTION

Objects of this class contain the metadata that describes a class in an
InterMine model.  Each class has a name, parent classes/interfaces and any
number of attributes, references and collections.

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Model::ClassDescriptor

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

=head2 new

 Usage   : my $cd = new InterMine::Model::ClassDescriptor(model => $model,
                             name => "Gene", extends => ["BioEntity"]);

 Function: create a new ClassDescriptor object
 Args    : model - the InterMine::Model that this class is a part of
           name - the class name
           extends - a list of the classes and interfaces that this classes
                     extends

=cut
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

  $self->{attributes} = [];
  $self->{fields} = [];
  $self->{references} = [];
  $self->{collections} = [];

  bless $self, $class;
  return $self;
}

=head2 
 
 Usage   : $cd->add_field($field); 
 Function: add a Field to this class
 Args    : $field - a sub class of InterMine::Model::Field

=cut
sub add_field
{
  # see also: Model->_fix_class_descriptors()

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

=head2 
 
 Usage   : $name = $cd->name();
 Function: Return the name of this class, eg. "org.flymine.model.genomic.Gene"
 Args    : none

=cut
sub name
{
  my $self = shift;
  return $self->{name};
}

=head2 
 
 Usage   : $name = $cd->name();
 Function: Return the unqualified name of this class, eg. "Gene"
 Args    : none

=cut
sub unqualified_name
{
  my $self = shift;
  return ($self->{name} =~ /.*\.(.*)/)[0];
}

=head2 
 
 Usage   : @parent_class_names = $cd->extends();
 Function: return a list of the names of the classes/interfaces that this class
           directly extends
 Args    : none

=cut
sub extends
{
  my $self = shift;
  return @{$self->{extends}};
}

=head2 
 
 Usage   : @parent_cds = $cd->extends_class_descriptors();
 Function: return a list of the ClassDescriptor objects for the
           classes/interfaces that this class directly extends
 Args    : none

=cut

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

=head2 
 
 Usage   : $field = $cd->get_field_by_name('company');
 Function: Return a Field object describing the given field, not undef if the
           field isn't a field in this class 
 Args    : $field_name - the name of the field to find

=cut
sub get_field_by_name
{
  # see also: Model->_fix_class_descriptors()

  my $self = shift;
  my $field_name = shift;

  return $self->{field_hash}{$field_name};
}

=head2 
 
 Usage   : if ($cd->valid_field('company')) { ... }
 Function: Return true if and only if the named field is a field in this class
 Args    : $field_name - the name of the field to find

=cut
sub valid_field
{
  my $self = shift;
  my $field = shift;
  return defined $self->get_field_by_name($field);
}

=head2 
 
 Usage   : @fields = $cd->attributes();
 Function: Return the Attribute objects for the attributes of this class
 Args    : none

=cut
sub attributes
{
  my $self = shift;
  return @{$self->{attributes}}
}

=head2 
 
 Usage   : @fields = $cd->fields();
 Function: Return the Attribute, Reference and Collection objects for all the
           fields of this class
 Args    : none

=cut
sub fields
{
  my $self = shift;
  return @{$self->{fields}}
}

=head2 
 
 Usage   : @fields = $cd->references();
 Function: Return the Reference objects for the references of this class
 Args    : none

=cut
sub references
{
  my $self = shift;
  return @{$self->{references}}
}

=head2 
 
 Usage   : @fields = $cd->collections();
 Function: Return the Collection objects for the collections of this class
 Args    : none

=cut
sub collections
{
  my $self = shift;
  return @{$self->{collections}}
}

=head2 
 
 Usage   : if ($class_desc->sub_class_of($other_class_desc)) { ... }
 Function: Returns true if and only if this class is a sub-class of the given
           class or is the same class
 Args    : $other_class_desc - a ClassDescriptor

=cut
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
