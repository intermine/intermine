package InterMine::Path;

=head1 NAME

InterMine::Path - an object representation of a path throw a model

=head1 SYNOPSIS

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Path

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2009 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use strict;

=head2 new

 Usage   : my $path = new InterMine::Path($model, "Department.company.name");
 Function: create a new Path object
 Args    : $model - the InterMine::Model to use for validating paths
           $path_string - the path in string format
=cut
sub new
{
  my $class = shift;

  if (@_ != 2) {
    die "Path::new needs two arguments\n";
  }

  my $model = shift;
  my $path_string = shift;

  my $self = {};

  $self->{model} = $model;
  $self->{string} = $path_string;

  $self->{parts} = [_get_parts($model, $path_string)];

  return bless $self, $class;
}

=head2 validate

 Usage   : InterMine::Path->validate($model, 'Department.name');
 Function: return true if and only if a path string is valid for the given model
 Args    : $model - the InterMine::Model to use for validating
           $path_string - the path in string format

=cut
sub validate
{
  my $class = shift;
  my $model = shift;
  my $path_string = shift;

  _get_parts($model, $path_string);
}

=head2 parts

 Usage   : my @parts = $path->parts();
 Function: Returns a list containing the ClassDescriptors and FieldDescriptors
           that describe this Part.  The first item in the list will always be
           a ClassDescriptor (eg. the "Department" descriptor for the path:
           "Department.name").  The last item in the list will be a
           Attribute object if the path ends in a field (eg. "Department.name")
           otherwise will be a Collection or a Reference object.  Other parts
           of the path are represented in the list as Collections or References.
 Example : the path "Department.company.name" has three parts, the "Department"
           ClassDescriptor, the "Department.company" Reference object and the
           "Company.name" Attribute object

=cut
sub parts
{
  my $self = shift;

  return @{$self->{parts}};
}

=head2 to_string

 Usage   : my $txt = $path->to_string();
 Function: return a text version of this path

=cut
sub to_string
{
  my $self = shift;

  $self->{string};
}

=head2 end

 Usage   : my $end_descriptor = $path->end();
 Function: return the field descriptor of the last part of the path, eg. for
           "Department.company.name" return the Company.name Attribute object.
           If the path consists only of a class (eg. "Department"), its
           ClassDescriptor is returned.

=cut
sub end
{
  my $self = shift;
  return @{$self->{parts}}[-1];
}

=head2 end_type

 Usage   : my $end_type = $path->end_type();
 Function: return the (Java) type of the last part of the path, eg. "String",
           "Integer", "Gene"

=cut
sub end_type
{
  my $self = shift;

  my $end = $self->end();

  if (ref $end eq 'InterMine::Model::Reference' ||
      ref $end eq 'InterMine::Model::Collection') {
    return $end->referenced_type_name();
  } else {
    if (ref $end eq 'InterMine::Model::Attribute') {
      return $end->attribute_type();
    } else {
      return $end->unqualified_name();
    }
  }
}

sub _get_parts
{
  my $model = shift;
  my $path_string = shift;

  my @parts = ();

  my @bits = split /[\.:]/, $path_string;

  my $top_class_name = shift @bits;

  my $top_class = $model->get_classdescriptor_by_name($top_class_name);

  if (defined $top_class) {
    push @parts, $top_class;
  } else {
    die qq[illegal path ($path_string), "$top_class_name" is not in the model]
  }

  my $current_class = $top_class;
  my $current_field = undef;

  for my $bit (@bits) {
    $current_field = $current_class->get_field_by_name($bit);

    if (!defined $current_field) {
      my $current_class_name = $current_class->name();
      die qq[can't find field "$bit" in class $current_class_name\n];
    }

    push @parts, $current_field;

    if ($current_field->field_type() eq 'attribute') {
      # if this is not the last bit, it will caught next time around the loop
      $current_class = undef;
    } else {
      $current_class = $current_field->referenced_classdescriptor();
    }
  }

  return @parts;
}

1;
