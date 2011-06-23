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

=cut



package InterMine::Model::Reference;
use Moose;
with (
    'InterMine::Model::Role::Descriptor',
    'InterMine::Model::Role::Field',
);

use MooseX::Types::Moose qw(Str Maybe);
use InterMine::Model::Types qw(
    ClassDescriptor MaybeClassDescriptor MaybeField
);

=head1 CONSTANTS

=head2 TAG_NAME

the name for serialising references to xml

=cut

use constant TAG_NAME => "reference";

=head1 ATTRIBUTES

=head2 referenced_type_name (Str, ro)

The name of the class of object this reference points to

=cut

has referenced_type_name => (
    is	     => 'ro',
    isa	     => Str,
    required => 1,
);

=head2 referenced_classdescriptor (ClassDescriptor, ro)

The class of the object that this reference points to

=cut

has referenced_classdescriptor => (
    is => 'ro',
    isa => ClassDescriptor,
    lazy => 1,
    default => sub {
        my $self      = shift;
        my $type_name = $self->referenced_type_name();
        return $self->model->get_classdescriptor_by_name($type_name);
    },
);

=head2 rev_referenced_classdescriptor (ClassDescriptor, ro)

The class that the other end of this reference points to.

=cut

has rev_referenced_classdescriptor => (
    is => 'ro',
    isa => MaybeClassDescriptor,
    lazy => 1,
    default => sub {
        my $self = shift;
        my $name = $self->name();
        return $self->model->get_referenced_classdescriptor($name);
    },
);

=head2 reverse_reference (Reference, ro)

The reference (if it exists) that points back to this one

=head3 predicate: has_reverse_reference

=cut

has reverse_reference => (
    is	      => 'ro',
    isa	      => MaybeField,
    lazy      => 1,
    default   => sub {
        my $self		   = shift;
        return undef unless $self->has_reverse_reference;
        my $referenced_cd	   = $self->referenced_classdescriptor();
        my $reverse_reference_name = $self->reverse_reference_name();
        return $referenced_cd->get_field_by_name($reverse_reference_name);
    },
);

=head2 reverse_reference_name (Str, ro)

The name of the reference that points back to this

=cut

has reverse_reference_name => (
    is	      => 'ro',
    isa	      => Maybe[Str],
    predicate => 'has_reverse_reference',
);

=head1 METHODS

=head2 is_many_to_many

Return true if this reference is one end of a many-to-many relation,
ie. this end is a collection and the other end is a collection

=cut

sub is_many_to_many {
  my $self = shift;
  return (
      $self->isa('InterMine::Model::Collection')
      and $self->has_reverse_reference
      and $self->reverse_reference->isa('InterMine::Model::Collection')
  );
}

=head2 is_many_to_one

Return true if this is the reference end of a one-to-many relation,
ie. this end is a reference and the other end is a collection

=cut

sub is_many_to_one {
  my $self = shift;
  return (
      not $self->isa('InterMine::Model::Collection')
      and $self->has_reverse_reference()
      and $self->reverse_reference()->isa('InterMine::Model::Collection')
  );
}

=head2 is_many_to_0

Return true if this is a collection and there is no reverse reference

=cut

sub is_many_to_0 {
  my $self = shift;
  return (
      $self->isa( 'InterMine::Model::Collection' )
      and not $self->has_reverse_reference
  );
}

=head2 is_one_to_many

Return true if this is the collection end of a one-to-many relation,
ie. this end is a collection and the other end is a reference

=cut

sub is_one_to_many {
  my $self = shift;
  return (
      $self->isa('InterMine::Model::Collection')
      and $self->has_reverse_reference
      and not $self->reverse_reference->isa('InterMine::Model::Collection')
  );
}

=head2 is_one_to_0

Return true if this is a reference and there is no reverse reference

=cut

sub is_one_to_0 {
  my $self = shift;
  return (
      not $self->isa('InterMine::Model::Collection')
      and not $self->has_reverse_reference);
}

sub _get_moose_type {
    my $self = shift;
    return $self->referenced_type_name;
}

around '_get_moose_options' => sub {
    my $orig = shift;
    my $self = shift;
    my @ops = $self->$orig(@_);
    push @ops, (coerce => 1);
    return @ops;
};

=head2 to_xml

Return the xml representation of the attribute descriptor

=cut

sub to_xml {
    my $self = shift;
    return sprintf(qq{<%s name="%s" referenced-type="%s" %s/>},
        $self->TAG_NAME,
        $self->name, 
        $self->referenced_type_name,
        ($self->reverse_reference_name 
            ? 'reverse-reference="' . $self->reverse_reference_name . '"'
            : ''),
    );
}

__PACKAGE__->meta->make_immutable;
no Moose;

1;

__END__

=head1 SEE ALSO

=over 4

=item L<InterMine::Model::ClassDescriptor>

=item L<InterMine::Model::Role::Field>

=item L<InterMine::Model::Role::Descriptor>

=back

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

Copyright 2006,2007,2008,2009,2010,2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.
