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

use Moose;

use InterMine::TypeLibrary qw(
    FieldList FieldHash ClassDescriptorList Model ClassDescriptor
);
use MooseX::Types::Moose qw(ArrayRef Str);

=head2 new

 Usage   : my $cd = new InterMine::Model::ClassDescriptor(model => $model,
                             name => "Gene", parents => ["BioEntity"]);

 Function: create a new ClassDescriptor object
 Args    : model   - the InterMine::Model that this class is a part of
           name    - the class name
           parents - a list of the classes and interfaces that this classes
                     extends

=cut

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


has own_fields => (
    traits => ['Array'],
    is	    => 'ro',
    isa	    => FieldList,
    default => sub { [] },
    handles => {
	add_own_field  => 'push',
	get_own_fields => 'elements',
    },
);
has fieldhash => (
    traits  => [qw/Hash/],
    is	    => 'ro',
    isa	    => FieldHash,
    default => sub { {} },
    handles => {
	set_field	  => 'set',
	get_field_by_name => 'get',
	fields		  => 'values',
	valid_field       => 'defined',
    },
);

has parents => (
    is	       => 'ro',
    isa	       => ArrayRef[Str],
    auto_deref => 1,
);


=head2 add_field

 Usage   : $cd->add_field($field);
 Function: add a Field to this class
 Args    : $field - a sub class of InterMine::Model::Field

=cut

# see also: Model->_fix_class_descriptors()
sub add_field {
  my ($self, $field, $own)  = @_;

  return if defined $self->get_field_by_name($field->name);

  $self->set_field($field->name, $field);
  $self->add_own_field($field) if $own;
}

has ancestors => (
    reader     => 'get_ancestors',
    isa	       => ClassDescriptorList,
    lazy       => 1,
    auto_deref => 1,
    default => sub {
	my $self = shift;
	my @inheritance_path = ($self,);
	my @classes = $self->parental_class_descriptors();
	for my $class (@classes) {
	    push @inheritance_path, $class->get_ancestors;
	}
	return \@inheritance_path;
    },
);


=head2 name

 Usage   : $name = $cd->name();
 Function: Return the name of this class, eg. "Gene"
 Args    : none


=head2 parents

 Usage   : @parent_class_names = $cd->parents();
 Function: return a list of the names of the classes/interfaces that this class
           directly extends
 Args    : none

=head2 parental_class_descriptors

 Usage   : @parent_cds = $cd->parental_class_descriptors();
 Function: return a list of the ClassDescriptor objects for the
           classes/interfaces that this class directly extends
 Args    : none

=cut

has parental_class_descriptors => (
    is	       => 'ro',
    isa	       => ClassDescriptorList,
    lazy       => 1,
    auto_deref => 1,
    default => sub {
	my $self = shift;
	return [map {$self->model->get_classdescriptor_by_name($_)}
		    $self->parents];
    },
);

# see FieldHash above

=head2 fields

 Usage   : @fields = $cd->fields();
 Function: Return the Attribute, Reference and Collection objects for all the
           fields of this class
 Args    : none

=head2 get_field_by_name

 Usage   : $field = $cd->get_field_by_name('company');
 Function: Return a Field object describing the given field, not undef if the
           field isn't a field in this class
 Args    : $field_name - the name of the field to find

=head2 valid_field

 Usage   : if ($cd->valid_field('company')) { ... }
 Function: Return true if and only if the named field is a field in this class
 Args    : $field_name - the name of the field to find


=head2 attributes

 Usage   : @fields = $cd->attributes();
 Function: Return the Attribute objects for the attributes of this class
 Args    : none

=cut

sub attributes {
    my $self = shift;
    return grep {$_->isa('InterMine::Model::Attribute')} $self->fields;
}

=head2 references

 Usage   : @fields = $cd->references();
 Function: Return the Reference objects for the references of this class
 Args    : none

=cut

sub references {
    my $self = shift;
    return grep {$_->isa('InterMine::Model::Reference')} $self->fields;
}

=head2 collections

 Usage   : @fields = $cd->collections();
 Function: Return the Collection objects for the collections of this class
 Args    : none

=cut

sub collections {
    my $self = shift;
    return grep {$_->isa('InterMine::Model::Collection')} $self->fields;
}

=head2 sub_class_of

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
    for my $parent ($self->parental_class_descriptors()) {
      if ($parent->sub_class_of($other_class_desc)) {
        return 1;
      }
    }
  }
  return 0;
}

1;
