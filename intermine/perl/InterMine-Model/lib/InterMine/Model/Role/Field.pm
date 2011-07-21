package InterMine::Model::Role::Field;

=head1 NAME

InterMine::Model::Role::Field - Provides common behaviour for field descriptors

=head1 SYNOPSIS

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Model::Role::Field

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

=head2 new

 Usage   : this is an abstract class, construct an Attribute, Collection or
           Reference instead
 Function: create a new Field object
 Args    : args are passed in as:  name => "value"
           name - the field name
           model - the Model
           type - for attributes, the type of the field (eg. String, Integer)
           referenced_type_name - for references and collections, the type of
                                  the referenced object(s)
           reverse_reference_name - for references and collections, the field
                                    name of the reverse reference


=cut 


use Moose::Role;

use InterMine::Model::Types qw(ClassDescriptor);
use MooseX::Types::Moose qw(Str);
use Scalar::Util qw(refaddr);

requires '_get_moose_type';

=head2 name (Str, ro, required)

The name of the descriptor

=cut 

sub name;

has name => (
    is => 'ro',
    isa => Str,
    required => 1,
);

=head2 field_class

 Usage   : my $class = $field->field_class();
 Function: returns the ClassDescriptor of the (base) class that defines this
           field

=cut

has field_class => (
    is	     => 'rw',
    isa	     => ClassDescriptor,
    weak_ref => 1,
    handles => {
        class_name => 'unqualified_name',
    },
);

sub _get_moose_options {
    my $self = shift;
    return (isa => $self->_get_moose_type);
}

sub _type_is {
    my $self = shift;
    my $something = shift;
#    if (refaddr($something)) {
#        return (
#            refaddr($self->_get_moose_type)
#            && 
#            refaddr($something) == refaddr($self->_get_moose_type)
#        );
#    } else {
        return ($something eq $self->_get_moose_type);
#    }
}

=head1 METHODS 

=head2 to_string

The string representation of a field descriptor, ie. its name

=cut 

sub to_string {
    my $self = shift;
    return $self->name;
}

no Moose;

1;
