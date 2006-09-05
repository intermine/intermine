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
    if ($key ne "model" && $key ne "id" && $key ne "classname") {
      die "unknown argument to $class->new(): $key\n";
    }
  }

  my $classname = $opts{classname};

  my $self = { id => $opts{id}, _model => $opts{model}, _classname => $classname };

  my $classdesc = $self->{_model}->get_classdescriptor_by_name($classname);

  if (!defined $classdesc) {
    die "class '$classname' is not in the model\n";
  }

  $self->{_classdesc} = $classdesc;

  bless $self, $class;

  return $self;
}

sub set
{
  my $self = shift;
  my $name = shift;
  my $value = shift;

  if (!defined $value) {
    die "value undefined for $name\n";
  }

  my $field = $self->{_classdesc}->get_field_by_name($name);

  if (!defined $field) {
    die $self->{_classname}, " does not have a $name field\n";
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

sub as_xml
{
  my $self = shift;
  my $writer = shift;
  my $id = $self->{id};
  my $class = $self->{_classname};

  $writer->startTag("item", id => $ID_PREFIX . $id, class => $class);

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
            $writer->emptyTag("reference", name => $key, ref_id => $ID_PREFIX . $r->{id});
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
