package InterMine::Model::ClassDescriptor;

=head1 NAME

InterMine::Model::ClassDescriptor - represents a class in an InterMine model

=head1 SYNOPSIS

 use InterMine::Model::ClassDescriptor;

 ...
 my $cd = InterMine::Model::ClassDescriptor->create(
            "Gene" => (
                model => $model,
                parents => ["BioEntity"]
            )
        );

=head1 DESCRIPTION

Objects of this class contain the metadata that describes a class in an
InterMine model.  Each class has a name, parent classes/interfaces and any
number of attributes, references and collections. 

InterMine class descriptors are sub classes of L<Moose::Meta::Class>, 
and thus L<Class::MOP::Class>. Please refer to these packages for further 
documentation.

=cut

use Moose;
extends qw/Moose::Meta::Class/;
with 'InterMine::Model::Role::Descriptor';

use InterMine::Model::Types qw(
    FieldHash ClassDescriptorList ClassDescriptor BigInt
);
use MooseX::Types::Moose qw(ArrayRef Str Bool);
use Moose::Util::TypeConstraints;
use Scalar::Util qw(refaddr blessed);

use Carp qw(cluck);

=head1 CLASS METHODS

=head2 create( $name | $name, %attributes | $name, \%attributes | \%attributes )

The class constructor inherited from L<Moose::Meta::Class>. Creates a new 
ClassDescriptor metaclass.

  my $cd = InterMine::Model::ClassDescriptor->create(
       "Gene" => (
           model => $model,
           parents => ["BioEntity"]
       )
  );

Params:
=over 4
=item model - the InterMine::Model that this class is a part of
=item name - the class name
=item parents - a list of the classes and interfaces that this classes extends
=back

In most normal use cases, the typical user should NOT need 
to call this method. It is used internally when parsing the 
model to build up the list of classes.

=cut

override create => sub {
    my $class = shift;
    my $ret = super;
    $ret->superclasses($ret->superclasses(), 'InterMine::Model::Object');
    return $ret;
};

=head1 INSTANCE ATTRIBUTES

=head2 name | package

=over 4

=item * unqualified_name

returns the (unqualified) name of the class this class descriptor represents. 

  $gene_meta->unqualified_name
  # "Gene"

=item * package

This is the attribute inherited from Moose::Meta::Class, and returns the full
qualified class name that perl refers to the class internally as.

  $gene_meta->name
  # InterMine::genomic::FlyMine::Gene

=back 

=cut

has unqualified_name => (
    isa => 'Str',
    is  => 'ro',
    init_arg => undef,
    lazy_build => 1,
);

sub _build_unqualified_name {
    my $self = shift;
    my $p = $self->name;
    $p =~ s/.*:://;
    return $p;
}

has is_interface => (
    isa => 'Bool',
    is  => 'ro',
    default => 1,
);

=head2 own_fields

Fields that belong to this class directly. 

=head3 add_own_field($field)

Add a field to the list

=head3 get_own_fields 

Get the full list of fields declared in this class.

=cut 

sub add_own_field {
    my $self = shift;
    my $field = shift;
    $self->set_field($field->name, $field);
    $field->field_class($self);
}

sub get_own_fields {
    my $self = shift;
    return grep {$_->field_class eq $self} $self->fields;
}

=head3 own_attributes

Return the fields that are instances of L<InterMine::Model::Attribute>. 
This is not to be confused with L<Class::MOP>'s C<get_all_attributes>.

=cut

sub own_attributes {
    my $self = shift;
    return grep {$_->isa('InterMine::Model::Attribute')} $self->get_own_fields;
}

=head3 own_references

Return all the fields that are instances of L<InterMine::Model::Reference>,
but not the subclass L<InterMine::Model::Collection>.

=cut

sub own_references {
    my $self = shift;
    return grep {
        $_->isa('InterMine::Model::Reference') && ! $_->isa('InterMine::Model::Collection')
        } $self->get_own_fields;
}

=head3 own_collections

Return all the fields that are instances of L<InterMine::Model::Collection>.

=cut

sub own_collections {
    my $self = shift;
    return grep {$_->isa('InterMine::Model::Collection')} $self->get_own_fields;
}

=head2 fieldhash

The map of fields for this class, including inherited fields. 
It has the following accessors:

=head3 set_field($name, $field)

Set a field in the map

=head3 get_field_by_name($name)

Retrieve the named field.

=head3 fields

Retrieve all fields as a list

=head3 valid_field($name)

Returns true if there is a field of this name 

=cut

has fieldhash => (
    traits  => [qw/Hash/],
    is	    => 'ro',
    isa	    => FieldHash,
    default => sub { {} },
    handles => {
        set_field	      => 'set',
        get_field_by_name => 'get',
        fields            => 'values',
        valid_field       => 'defined',
    },
);

=head3 attributes

Return the fields that are instances of L<InterMine::Model::Attribute>. 
This is not to be confused with L<Class::MOP>'s C<get_all_attributes>.

=cut

sub attributes {
    my $self = shift;
    return grep {$_->isa('InterMine::Model::Attribute')} $self->fields;
}

=head3 references

Return all the fields that are instances of L<InterMine::Model::Reference>,
but not the subclass L<InterMine::Model::Collection>.

=cut

sub references {
    my $self = shift;
    return grep {
        $_->isa('InterMine::Model::Reference') && ! $_->isa('InterMine::Model::Collection')
        } $self->fields;
}

=head3 collections

Return all the fields that are instances of L<InterMine::Model::Collection>.

=cut

sub collections {
    my $self = shift;
    return grep {$_->isa('InterMine::Model::Collection')} $self->fields;
}


=head2 parents 

The names of the immediate ancestors of this class.

=cut

has parents => (
    is	       => 'ro',
    isa	       => ArrayRef[Str],
    traits     => ['Array'],
    auto_deref => 1,
    handles    => {
        has_parents => 'count',
    },

);

=head2 parental_class_descriptors

return a list of the ClassDescriptor objects for the
classes/interfaces that this class directly extends

 my @parent_cds = $cd->parental_class_descriptors();

Calling this method retrives the parents from the model
and also sets up superclass relationships
in Moose. It should not be called until the Model is completely
parsed. It is called automatically once the model has been 
parsed.

=cut

has parental_class_descriptors => (
    is	       => 'ro',
    isa	       => ClassDescriptorList,
    lazy       => 1,
    auto_deref => 1,
    default => sub {
        my $self = shift;
        $self->superclasses($self->parents);
        return [ map {$self->model->get_classdescriptor_by_name($_)} 
                $self->parents ];
    },
);

has _is_ready => (
    is => 'ro',
    isa => Bool,
    default => 0,
    writer => '_set_fixed',
);

=head1 INSTANCE METHODS

=head2 new_object

The instantiation method inherited from L<Moose::Meta::Class>.
You should not normally need to use this directly. Instead call
the C<make_new> method in L<InterMine::Model>.

=cut

# sanitize input by removing undef attributes from the list

around new_object => sub {
    my $orig = shift;
    my $self = shift;
    my $args = (ref $_[0] eq 'HASH') ? $_[0] : {@_};
    for my $key (keys %$args) {
        my $value = $args->{$key};
        delete $args->{$key} unless (defined $value);
        # Horrible hacky solution to unnecessary warnings
        # THIS SHOULD BE DELETED WHEN A BETTER COERCION 
        # SOLUTION CAN BE FOUND!!!!
        if (blessed $value and $value->isa('JSON::Boolean')) {
            $args->{$key} = $$value;
        }
    }
    return $self->$orig($args);
};

# and make name a synonym for package here.


=head2 get_ancestors

The full inheritance list, including all ancestors in the model.

=cut

# Implemented as a method to avoid memory leaks
sub get_ancestors {
    my $self = shift;
    my @inheritance_path = ($self,);
    my @classes = $self->parental_class_descriptors();
    for my $class (@classes) {
        push @inheritance_path, $class->get_ancestors;
    }
    return @inheritance_path;
}

=head2 add_field(FieldDescriptor $field, Bool own)

Add a field to the class. If there is already a field 
of the same name, it will not be added twice. Setting the boolean
flag "own" marks this field as originating in this class

See also: InterMine::Model->_fix_class_descriptors

=cut

sub add_field {
  my ($self, $field, $own)  = @_;

  return if defined $self->get_field_by_name($field->name);

  if ($own) {
    $self->add_own_field($field);
  } else {
    $self->set_field($field->name, $field);
  }
}

use Moose::Util::TypeConstraints;

sub _make_fields_into_attributes {
    my $self   = shift;
    my @fields = $self->fields;

    for my $field (@fields) {
        my $suffix = ucfirst($field->name);
        my $get = $field->_type_is(Bool) ? 'is' : 'get';
        my $options = {
            reader    => $get  . $suffix,
            writer    => "set" . $suffix,
            predicate => "has" . $suffix,
            $field->_get_moose_options,
        };

        my $isa = Moose::Util::TypeConstraints::find_type_constraint($options->{isa});
        unless ($isa) {
            $self->model->get_classdescriptor_by_name($field->referenced_type_name);
        }

        $self->add_attribute($field->name, $options);
        my $been_fetched = undef;
        $self->add_method($field->name, sub { my $obj = shift; 
            my $reader = $get . $suffix; 
            my $writer = "set" . $suffix;
            my $is_empty = $field->name . "_is_empty";
            if (not ($been_fetched)
                and ( 
                     ($field->isa("InterMine::Model::Reference")  and (not defined $obj->$reader))
                  or ($field->isa("InterMine::Model::Collection") and ($obj->$is_empty))
                )) {
                my $fetched = $self->model->lazy_fetch($self, $field, $obj);
                $obj->$writer($fetched) if $fetched;
                $been_fetched = 1;
            }
            $obj->$reader;
        });
    }
}

=head2 sub_class_of

Returns true if and only if this class is a sub-class 
of the given class or is the same class

 if ($cld->sub_class_of($other_cld)) { ... }

=cut

sub sub_class_of {
  my $self = shift;
  my $other_class_desc = shift;

  if ($self eq $other_class_desc) {
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

=head2 superclass_of($other)

Returns true if this class is a superclass of the other one, or if it 
is the same class.

=cut

sub superclass_of {
    my $self = shift;
    my $other = shift;

    if (blessed $other and $other->isa(__PACKAGE__)) {
        return $other->sub_class_of($self);
    } else {
        return $self->model->get_classdescriptor_by_name($other)->sub_class_of($self);
    }
}

=head2 to_string

The stringification of a class-descriptor. By default, it stringifies to its 
unqualified_name.

=cut 

sub to_string {
    my $self = shift;
    return $self->unqualified_name;
}

=head2 to_xml

Returns a string containing an XML representation of the descriptor

=cut

sub to_xml {
    my $self = shift;
    my $xml = sprintf(qq{<class name="%s"%s is-interface="%s">\n},
        $self->unqualified_name, 
        ($self->has_parents 
            ? ' extends="' . join(q[ ], $self->parental_class_descriptors) . '"' 
            : ''),
        ($self->is_interface ? "true" : "false")
    );
    for my $field (
        sort($self->own_attributes), 
        sort($self->own_references), 
        sort($self->own_collections)) {
        $xml .= q[ ] x 4 . $field->to_xml . "\n";
    }
    $xml .= "  </class>";
    return $xml;
}

1;

=head1 SEE ALSO

=over 4

=item * L<Moose::Meta::Class>

=back

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

