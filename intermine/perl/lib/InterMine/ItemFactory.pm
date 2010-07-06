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

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::ItemFactory

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

our $VERSION = '0.03';

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
  my $self = { id_counter => 0, %opts, package_name => $model->package_name() };

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

  my $classname = '';
  if (defined $args{classname}) {
    $classname = $args{classname};
  }

  my $implements = '';
  if (defined $args{implements}) {
    $implements = $args{implements};
  }

  return new InterMine::Item(classname => $classname, implements => $implements,
                             model => $self->{model},
                             id => $self->{id_counter});
}

1;
