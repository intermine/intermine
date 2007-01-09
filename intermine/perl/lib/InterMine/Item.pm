package InterMine::Item;

use strict;

use XML::Writer;

my $ID_PREFIX = '0_';

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
              id => $opts{id}, _model => $opts{model}, _classname => $classname,
              _implements => $implements_arg
             };

  if ($classname ne '') {
    my $classdesc = $self->{_model}->get_classdescriptor_by_name($classname);
    $self->{_classdesc} = $classdesc;
  }

  my @implements_classdescs = map {
    my $imp_classdesc = $self->{_model}->get_classdescriptor_by_name($_);
    if (!defined $imp_classdesc) {
      die "interface '$_' is not in the model\n";
    }
    $imp_classdesc;
  } @implements;

  if ($classname eq '' and scalar(@implements_classdescs) == 0) {
    die "no '$classname' and no implementations for object\n";
  }

  $self->{_implements} = $implements_arg;
  $self->{_implements_classdescs} = [@implements_classdescs];
  $self->{_classname} = $classname;

  bless $self, $class;

  return $self;
}

sub get_object_field_by_name
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

sub set
{
  my $self = shift;
  my $name = shift;
  my $value = shift;

  if (!defined $value) {
    die "value undefined for $name\n";
  }

  my $field = $self->get_object_field_by_name($name);

  if (!defined $field) {
    my @class_descs = $self->all_class_descriptors();
    my $classes = join ' ', map { $_->name() } @class_descs;
    die "object ", $self->get('id'), " ($classes) cannot have a $name field\n";
  }

  $self->{$name} = $value;

  if (ref $value) {
    if (ref $value eq 'ARRAY') {
      if (ref $field ne 'InterMine::Model::Collection') {
        die "tried to set field '$name' in class '", $self->to_string(),
            "' to something other than type: ", $field->field_type(), "\n";
      }

      # check the types of the elements in the collection and set the reverse
      # references if necessary
      my @items = @$value;
      for my $other_item (@items) {
        if ($other_item->instance_of($field->referenced_classdescriptor())) {
          if ($field->is_one_to_many()) {
            $other_item->set($field->reverse_reference_name(), $self);
          } else {
            if ($field->is_many_to_many()) {
              my @other_collection = @{$other_item->get($field->reverse_reference_name())};
              if (!grep {$_ == $self} @other_collection) {
                push @other_collection, $self;
                $other_item->set($field->reverse_reference_name(), \@other_collection);
              }
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
    }
  } else {
    if (ref $field ne 'InterMine::Model::Attribute') {
      die "tried to set field '$name' in class '", $self->to_string(),
          "' to something other than type: ", $field->field_type(), "\n";
    }
  }
}

sub get
{
  my $self = shift;
  my $fieldname = shift;
  my $field = $self->get_object_field_by_name($fieldname);

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

sub model
{
  my $self = shift;
  return $self->{_model};
}

sub classname
{
  my $self = shift;
  return $self->{_classname};
}

sub classdescriptor
{
  my $self = shift;
  return $self->{_classdesc};
}

sub implements_classdescriptors
{
  my $self = shift;
  return @{$self->{_implements_classdescs}};
}

sub all_class_descriptors
{
  my $self = shift;

  my @class_descs = $self->implements_classdescriptors();
  if (defined $self->classdescriptor()) {
    push @class_descs, $self->classdescriptor();
  }
  return @class_descs;
}


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

sub to_string
{
  my $self = shift;
  my $implements = join (' ', $self->{_implements});
  my $classname = $self->classname();
  if (defined $classname and length $classname > 0) {
    return "[classname: " . $classname . "  implements: $implements]";
  } else {
    return "[implements: $implements]";
  }
}

sub as_xml
{
  my $self = shift;
  my $writer = shift;
  my $id = $self->{id};
  my $classname = $self->{_classname} || "";
  my $implements = $self->{_implements} || "";

  $writer->startTag("item", id => $ID_PREFIX . $id,
                    class => $classname, implements => $implements);

  for my $key (keys %$self) {
    next if $key =~ /^_/;
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

1;
