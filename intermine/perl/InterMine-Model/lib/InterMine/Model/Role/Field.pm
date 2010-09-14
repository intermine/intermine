
=head1 NAME

InterMine::Model::Field - Representation of a field of a class

=head1 SYNOPSIS

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Model::Field

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

=head2 name

 Usage   : $name = $field->name();
 Function: return the name of this field

=cut

package InterMine::Model::Role::Field;

use Moose::Role;

use InterMine::TypeLibrary qw(ClassDescriptor Model);
use MooseX::Types::Moose qw(Str);

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

=head2 field_class

 Usage   : my $class = $field->field_class();
 Function: returns the ClassDescriptor of the (base) class that defines this
           field

=cut

has field_class => (
    is	     => 'rw',
    isa	     => ClassDescriptor,
);

no Moose;

1;
