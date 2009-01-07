package InterMine::Item;

=head1 NAME

InterMine::Item - Representation of InterMine items

=head1 SYNOPSIS

  my $factory = new InterMine::ItemFactory(model => $model);

  my $gene = $factory->make_item("Gene");
  $gene->set("identifier", "CG10811");

(See InterMine::ItemFactory for a longer Synopsis)

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Item

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=item * Documentation

L<http://www.intermine.org/wiki/ItemsAPIPerl>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use strict;

use XML::Writer;

my $ID_PREFIX = '0_';

=head2 new

 Title   : new
 Usage   : $item = $factory->make_item("Gene");   # calls Item->new() implicitly
 Function: create a new Item
 Args    : model - the InterMine::Model object to use to check field validity
 Note    : use this method indirectly using an ItemFactory
=cut

sub new {
  my $class = shift;
  my %opts = @_;

  if (!defined $opts{id}) {
    die "no id argument in $class constructor\n";
  }
  if (!defined $opts{model}) {
    die "no model argument in $class constructor\n";
  }
  for my $key (keys %opts) {
    if ($key ne "model" && $key ne "id" && $key ne "classname" && $key ne "implements") {
      die "unknown argument to $class->new(): $key\n";
    }
  }

  my $classname = $opts{classname} || "";
  my $implements_arg = $opts{implements} || "";

  my @implements = ();

  if (ref $implements_arg eq 'ARRAY') {
    @implements = @$implements_arg;
  } else {
    if ($implements_arg ne '') {
      @implements = split /\s+/, $implements_arg;
    }
  }

  my $self = {
              id => $opts{id}, ':model' => $opts{model}, ':classname' => $classname,
              ':implements' => $implements_arg
             };

  if ($classname ne '') {
    my $classdesc = $self->{':model'}->get_classdescriptor_by_name($classname);
    $self->{':classdesc'} = $classdesc;
  }

  my @implements_classdescs = map {
    my $imp_classdesc = $self->{':model'}->get_classdescriptor_by_name($_);
    if (!defined $imp_classdesc) {
      die "interface '$_' is not in the model\n";
    }
    $imp_classdesc;
  } @implements;

  if ($classname eq '' and scalar(@implements_classdescs) == 0) {
    die "no '$classname' and no implementations for object\n";
  }

  $self->{':implements'} = $implements_arg;
  $self->{':implements_classdescs'} = [@implements_classdescs];
  $self->{':classname'} = $classname;

  bless $self, $class;

  return $self;
}

sub _get_object_field_by_name
{
  my $self = shift;
  my $name = shift;

  my @class_descs = $self->all_class_descriptors();

  for my $class_desc (@class_descs) {
    if (defined $class_desc->get_field_by_name($name)) {
      return $class_desc->get_field_by_name($name);
    }
  }
  return undef;
}

=head2 set

 Title   : set
 Usage   : $gene_item->set("name", "wtf7");
       or: $gene_item->set("organism", $organism_item);
 Function: set a field in the Item, checking that this object can have a field
           with that name
 Args    : $name - the name of the field to set
           $value - the new value (must not be undefined)

=cut

sub set
{
  my $self = shift;
  my $name = shift;
  my $value = shift;

  if (!defined $value) {
    die "value undefined while setting $name\n";
  }

  my $field = $self->_get_object_field_by_name($name);

  if (!defined $field) {
    my @class_descs = $self->all_class_descriptors();
    my $classes = join ' ', map { $_->name() } @class_descs;
    die "object ", $self->to_string(), " does not have a field called: $name\n";
  }

  if (ref $value) {
    if (ref $value eq 'ARRAY') {
      if (ref $field ne 'InterMine::Model::Collection') {
        die "tried to set field '$name' in class '", $self->to_string(),
            "' to something other than type: ", $field->field_type(), "\n";
      }

      $self->{$name} = $value;

      my @items = @$value;

      my $collection_hash_name = _get_collection_hash_name($name);
      my %collection_hash = map {$_ => $_} @items;

      $self->{$collection_hash_name} = \%collection_hash;

      # check the types of the elements in the collection and set the reverse
      # references if necessary

      for my $other_item (@items) {
        if ($other_item->instance_of($field->referenced_classdescriptor())) {
          if ($field->is_one_to_many()) {
            my $current_rev_ref = $other_item->get($field->reverse_reference_name());
            if (!defined $current_rev_ref || $current_rev_ref != $self) {
              $other_item->set($field->reverse_reference_name(), $self);
            }
          } else {
            if ($field->is_many_to_many()) {
              $other_item->_add_to_collection($field->reverse_reference_name(), $self);
            }
          }
        } else {
          die "collection '$name' in class '", $self->to_string(),
            "' must contain items of type: ", $field->referenced_type_name(),
            " not: ", $self->to_string();
        }
      }
    } else {
      if (ref $field ne 'InterMine::Model::Reference') {
        die "tried to set field '$name' in class '", $self->to_string(),
           "' to something other than type: ", $field->field_type(), "\n";
      }

      if (!defined $self->{$name} || $self->{$name} != $value) {
        $self->{$name} = $value;

        if ($field->is_many_to_one()) {
          # add this Item to the collection in the other Item
          $value->_add_to_collection($field->reverse_reference_name(), $self);
        }
      }
    }
  } else {
    if (ref $field ne 'InterMine::Model::Attribute') {
      die "tried to set field '$name' in class '", $self->to_string(),
          "' to something other than type: ", $field->field_type(), "\n";
    }

    $self->{$name} = $value;
  }
}

=head2 get

 Title   : get
 Usage   : $gene_name = $gene_item->get("name");
       or: $organism_item = $gene_item->get("organism");
 Function: get the value of a field from an Item
 Args    : $name - the name of the field to get
 Return  : the value

=cut

sub get
{
  my $self = shift;
  my $fieldname = shift;
  my $field = $self->_get_object_field_by_name($fieldname);

  if (!defined $field) {
    die qq(object ") . $self->to_string() . qq(" doesn't have a field named: $fieldname\n);
  }

  my $retval = $self->{$fieldname};
  if (defined $retval) {
    return $retval;
  } else {
    if ($field->field_type() eq 'collection') {
      return [];
    } else {
      return undef;
    }
  }
}

sub _get_collection_hash_name
{
  my $name = shift;
  return ":${name}:hash";
}

sub _add_to_collection
{
  my $self = shift;
  my $name = shift;
  my $value = shift;

  my $field = $self->_get_object_field_by_name($name);

  if (ref $field ne 'InterMine::Model::Collection') {
    die "can't add $value to a field ($name in " . $self->to_string() .
        ") that isn't a collection\n";
  }

  if (ref $value ne 'InterMine::Item') {
    die qq(can't add value "$value" to a collection $name in ) . $self->to_string() .
        qq(as it isn't an Item\n);
  }

  my $collection_hash_name = _get_collection_hash_name($name);

  my %collection_hash;
  if (defined $self->{$collection_hash_name}) {
    %collection_hash = %{$self->{$collection_hash_name}}
  } else {
    %collection_hash = ();
  }

  if (exists $collection_hash{$value}) {
    return;
  }

  push @{$self->{$name}}, $value;
}

=head2 model

 Title   : model
 Usage   : $model = $item->model();
 Function: return the model that this Item obeys

=cut

sub model
{
  my $self = shift;
  return $self->{':model'};
}

=head2 classname

 Title   : classname
 Usage   : $classname = $item->classname();
 Function: return the class name of this Item - ie the class name that will be
           used when creating the object in InterMine

=cut

sub classname
{
  my $self = shift;
  return $self->{':classname'};
}

=head2 classdescriptor

 Title   : classdescriptor
 Usage   : $cd = $item->classdescriptor();
 Function: return the ClassDescriptor object from the model for this Item

=cut

sub classdescriptor
{
  my $self = shift;
  return $self->{':classdesc'};
}

sub _implements_classdescriptors
{
  my $self = shift;
  return @{$self->{':implements_classdescs'}};
}

=head2 all_class_descriptors

 Title   : all_class_descriptors
 Usage   : @cds = $item->all_class_descriptors();
 Function: return a list of ClassDescriptor objects from the model for this
           Item, including the classdescriptors of all parent objects

=cut

sub all_class_descriptors
{
  my $self = shift;

  my @class_descs = $self->_implements_classdescriptors();
  if (defined $self->classdescriptor()) {
    push @class_descs, $self->classdescriptor();
  }
  return @class_descs;
}

=head2 valid_field

 Title   : valid_field
 Usage   : if ($item->valid_field('someFieldName')) { ... };
 Function: return true if and only if the given field name is valid for this
           object according to the model

=cut

sub valid_field
{
  my $self = shift;
  my $field = shift;

  my @class_descs = $self->all_class_descriptors();

  for my $class_desc (@class_descs) {
    if ($class_desc->valid_field($field)) {
      return 1;
    }
  }

  return 0;
}

=head2 instance_of

 Title   : instance_of
 Usage   : my $gene_cd = $model->get_classdescriptor_by_name("Gene");
           if ($some_item->instance_of($gene_cd)) { ... }
 Function: Return true if and only if this Item represents an object that has
           the given class, or is a sub-class.

=cut


sub instance_of
{
  my $self = shift;
  my $other_class_desc = shift;

  for my $class_desc ($self->all_class_descriptors()) {
    if ($class_desc->sub_class_of($other_class_desc)) {
      return 1;
    }
  }
  return 0;
}

=head2 to_string

 Title   : to_string
 Usage   : warn('item: ', $item->to_string());
 Function: return a text representation of this Item

=cut

sub to_string
{
  my $self = shift;
  my $implements = join (' ', $self->{':implements'});
  my $classname = $self->classname();
  if (defined $classname and length $classname > 0) {
    return "[classname: " . $classname . "  implements: $implements]";
  } else {
    return "[implements: $implements]";
  }
}

=head2 as_xml

 Title   : as_xml
 Usage   : $xml = $item->as_xml();
 Function: return an XML representation of this Item

=cut

sub as_xml
{
  my $self = shift;
  my $writer = shift;
  my $id = $self->{id};
  my $classname = $self->{':classname'} || "";
  my $implements = $self->{':implements'} || "";

  $classname = _package_name_to_namespace($classname);
  my @implements_list = split ' ', $implements;
  $implements = join ' ', map { _package_name_to_namespace($_) } @implements_list;

  $writer->startTag("item", id => $ID_PREFIX . $id,
                    class => $classname, implements => $implements);

  for my $key (keys %$self) {
    next if $key =~ /^:/;
    if ($key ne 'id' && $key ne 'class') {
      my $val = $self->{$key};

      die unless $val;

      if (ref $val) {
        if (ref $val eq 'ARRAY') {
          $writer->startTag("collection", name => $key);
          my @refs = @$val;
          for my $r (@refs) {
            $writer->emptyTag("reference", ref_id => $ID_PREFIX . $r->{id});
          }
          $writer->endTag();
        } else {
          $writer->emptyTag("reference", name => $key, ref_id => $ID_PREFIX . $val->{id});
        }
      } else {
        $writer->emptyTag("attribute", name => $key, value => $val);
      }
    }
  }

  $writer->endTag();
}

sub _package_name_to_namespace
{
  my $package_name = shift;

  if ($package_name =~ m/^\s*$/) {
    return "";
  }

  if ($package_name =~ m/([^\.]+)\.([^\.]+)\.(.*)\.(.*)/) {
    my $tld = $1;
    my $domain = $2;
    my $pack_bits = $3;
    my $class_name = $4;

    $pack_bits =~ s@\.@/@g;

    return "http://www.$domain.$tld/$pack_bits#$class_name";
  } else {
    die "cannot understand package name: $package_name\n";
  }
}

1;
