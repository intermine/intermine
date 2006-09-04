package InterMine::Item;

use strict;

use XML::Writer;

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

  my $classdesc = $self->{_model}->get_class_by_name($classname);

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

  $self->{$name} = $value;
}

sub model
{
  my $self = shift;
  return $self->{_model};
}

sub as_xml
{
  my $self = shift;
  my $writer = shift;
  my $id = $self->{id};
  my $class = $self->{_classname};

  $writer->startTag("item", id => $id, class => $class);

  for my $key (keys %$self) {
    next if $key =~ /^_/;
    if ($key ne 'id' && $key ne 'class') {
      my $val = $self->{$key};

      die unless $val;

      if (ref $val) {
        die "$key" unless $val->{id};
        if (ref $val eq 'ARRAY') {
          $writer->startTag("collection", name => $key);
          my @refs = @$val;
          for my $r (@refs) {
            $writer->emptyTag("reference", name => $key, ref_id => '0_' . $r->{id});
          }
          $writer->endTag();
        } else {
          print "--- $key $val\n";
          $writer->emptyTag("reference", name => $key, ref_id => '0_' . $val->{id});
        }
      } else {
        $writer->emptyTag("attribute", name => $key, value => $val);
      }
    }
  }

  $writer->endTag();
}

1;
