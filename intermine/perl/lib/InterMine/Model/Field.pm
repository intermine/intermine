package InterMine::Model::Field;

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

use strict;

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
sub new
{
  my $class = shift;
  my %opts = @_;
  my $self = {%opts};

  if (exists $opts{type}) {
    my $type = $opts{type};
    $type =~ s/.*\.//;
    $self->{type} = $type;
  } else {
    if ($class eq 'InterMine::Model::Attribute') {
      die "no type specified for ", $self->{field_name}, "\n";
    }
  }

  bless $self, $class;
  return $self;
}

=head2

 Usage   : $name = $field->field_name();
 Function: return the name of this field

=cut
sub field_name
{
  my $self = shift;
  return $self->{name};
}

=head2

 Usage   : $name = $field->field_type();
 Function: return the type of this field, "Attribute", "Reference" or
           "Collection"

=cut
sub field_type
{
  my $self = shift;
  return lc (((ref $self) =~ /.*::(.*)/)[0]);
}

=head2

 Usage   : my $class = $field->field_class();
 Function: returns the ClassDescriptor of the (base) class that defines this
           field

=cut
sub field_class
{
  my $self = shift;
  if (@_ > 0) {
    $self->{field_class} = shift;
  } else {
    return $self->{field_class};
  }
}

1;
