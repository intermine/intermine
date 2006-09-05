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

  if (ref $value) {
    if (ref $value eq 'ARRAY') {
      if (ref $field ne 'InterMine::Model::Collection') {
        die "tried to set field '$name' in class '", $self->{_classname},
            " to something other than type: ", $field->get_field_type(), "\n";
      }
    } else {
      if (ref $field ne 'InterMine::Model::Reference') {
        die "tried to set field '$name' in class '", $self->{_classname},
           " to something other than type: ", $field->get_field_type(), "\n";
      }
    }
  } else {
    if (ref $field ne 'InterMine::Model::Attribute') {
      die "tried to set field '$name' in class '", $self->{_classname},
          " to something other than type: ", $field->get_field_type(), "\n";
    }
  }

  $self->{$name} = $value;
}

sub get
{
  my $self = shift;
  my $field = shift;
  return $self->{$field};
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
