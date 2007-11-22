package InterMine::ItemFactory;

=head1 NAME

InterMine::ItemFactory - factory for creating Item objects that match a given
Model

=head1 SYNOPSIS

  use XML::Writer;
  use InterMine::Model;
  use InterMine::Item;
  use InterMine::ItemFactory;

  my $model_file = $ARGV[0];
  die unless defined $model_file;
  my $model = new InterMine::Model(file => $model_file);
  my $factory = new InterMine::ItemFactory(model => $model);

  my $gene = $factory->make_item("Gene");
  # set an attribute
  $gene->set("identifier", "CG10811");

  my $organism = $factory->make_item("Organism");
  $organism->set("taxonId", 7227);

  # set a reference
  $gene->set("organism", $organism);

  my $pub1 = $factory->make_item("Publication");
  $pub1->set("pubMedId", 11700288);
  my $pub2 = $factory->make_item("Publication");
  $pub2->set("pubMedId", 16496002);

  # set a collection
  $gene->set("publications", [$pub1, $pub2]);

  # write as InterMine Items XML
  my @items_to_write = ($gene, $organism, $pub1, $pub2);
  my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3);
  $writer->startTag("items");
  for my $item (@items_to_write) {
    $item->as_xml($writer);
  }
  $writer->endTag("items");

=head1 DESCRIPTION

The class implements factory objects for create XML in InterMine Items XML
format for use as input for sources.

=cut

=head1 FUNCTIONS

=cut

use strict;

use InterMine::Item;

=head2 new

 Title   : new
 Usage   : $factory = new InterMine::ItemFactory($model);
 Function: return a factory that can be used to create Item objects
 Args    : model - the InterMine::Model object to use to check field validity

=cut

sub new
{
  my $class = shift;
  my %opts = @_;

  if (!exists $opts{model}) {
    die "-model argument missing in ItemFactory constructor\n";
  }
  my $model = $opts{model};
  my $self = { id_counter => 0, %opts, name_space => $model->name_space() };

  bless $self, $class;
  return $self;
}

=head2 make_item

 Title   : make_item
 Usage   : $item = $factory->make_item("Gene");
 Function: return a new Item object of the given type
 Args    : the classname of the new Item

=cut

sub make_item
{
  my $self = shift;
  my %args;

  if (@_ == 1) {
    $args{implements} = $_[0];
  } else {
    %args = @_;
  }

  $self->{id_counter}++;

  my $classname = "";
  if (defined $args{classname}) {
    if ($args{classname} =~ m;^http://;) {
      $classname = $args{classname};
    } else {
      $classname = $self->{model}->package_name() . '.' . $args{classname};
    }
  }

  my $implements = "";
  if (defined $args{implements}) {
    if ($args{implements} =~ m;^http://;) {
      $implements = $args{implements};
    } else {
      $implements = $self->{model}->package_name() . '.' . $args{implements};
    }
  }

  return new InterMine::Item(classname => $classname, implements => $implements,
                             model => $self->{model},
                             id => $self->{id_counter});
}

1;
