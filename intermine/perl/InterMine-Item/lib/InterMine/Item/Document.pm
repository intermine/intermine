package InterMine::Item::Document;

=head1 NAME

InterMine::Item::Document - a module for writing InterMine-Items-XML files 

=head1 SYNOPSIS

  use InterMine::Model;
  use InterMine::Item::Document;

  my ($model_file, $output_file) = @ARGV;

  my $model = InterMine::Model->new(file => $model_file);
 
  my $doc   = InterMine::Item::Document->new(
    model      => $model,
    output     => $output_file, # defaults to STDOUT
    auto_write => 1, # automatically write each item as it is made
  );

  my $organism = $doc->add_item(
    "Organism",
    "taxonId" => 7227
  );

  my $pub1 = $doc->add_item(
    "Publication",
    "pubMedId" => 11700288,
  );
  my $pub2 = $doc->make_item(
    "Publication",
    "pubMedId" => 16496002,
  );

  $doc->add_item(
    'Gene',
    'identifier'   => "CG10811",
    'organism'     => $organism,
    'publications' => [$pub1, $pub2],
  );

  $doc->close; # writes the end tags - only needed when auto_write is on

=head1 DESCRIPTION

This module allows you to make and write out InterMine-Items-XML for 
integrating data into InterMine databases

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

use XML::Writer;
use InterMine::Item;

=head2 new

 Title   : new
 Usage   : $factory = new InterMine::ItemFactory($model);
 Function: return a factory that can be used to create Item objects
 Args    : model - the InterMine::Model object to use to check field validity

=cut
sub new {
  my $class = shift;
  my %opts = @_;

  if (!exists $opts{model}) {
    die "model argument missing in Item::Document constructor\n";
  }
  my %writer_args = (
        DATA_MODE   => 1, 
        DATA_INDENT => 3, 
    );
  if (my $out_file = delete $opts{output}) {
     open(my $output, '>', $out_file) 
        or die "Cannot open $out_file for writing, $!";
    $writer_args{OUTPUT} = $output;
  }
  $opts{writer} = new XML::Writer(%writer_args);
  my $model     = $opts{model};
  $opts{unwritten_items}  = [];

  my $self = { id_counter => 0, %opts, package_name => $model->package_name() };

  bless $self, $class;
  return $self;
}

=head2 writer

 Function : Return the writer for this document

=cut

sub writer {
    my $self = shift;
    return $self->{writer};
}

=head2 write

 Function : write all unwritten items to the xml output. This is 
            called automatically on item creation if auto_write is 
            set.

=cut

sub write {
    my $self = shift;
    push @{$self->{unwritten_items}}, @_;

    return unless @{$self->{unwritten_items}};
    my $writer = $self->{writer};
    
    unless ($writer->within_element('items')) {
        $writer->startTag('items');
    }
    while (my $item = shift @{$self->{unwritten_items}}) {
        $item->as_xml($writer);
    }
    return;
}

sub DESTROY {
    my $self = shift;
    if ($self->writer->within_element('items')) {
        $self->close;
    }
}

=head2 close

 Function : close the document by writing any unwritten
            items and closing the items tag.

=cut

sub close {
    my $self = shift;
    $self->write();
    if ($self->writer->within_element('items')) {
        $self->writer->endTag('items');
    }
    return;
}
    
=head2 make_item

 Title   : make_item
 Usage   : $item = $doc->make_item("Gene", [%attributes]);
 Function: return a new Item object of the given type
           while not storing it 
 Args    : the classname of the new Item
           Any attributes for the item

=cut

sub make_item {
  my $self = shift;
  my %args;
  my %attr;

  if (@_ == 1) {
    $args{implements} = shift;
  } elsif (@_ % 2 == 1) {
      $args{implements} = shift;
      %attr = @_;
  } else {
    %args = @_;
  }

  $self->{id_counter}++;

  my $classname  = $args{classname}  || '';
  my $implements = $args{implements} || '';

  my $item = new InterMine::Item(
      classname  => $classname, 
      implements => $implements,
      model      => $self->{model},
      id         => $self->{id_counter}
  );

  while (my ($k, $v) = each %attr) {
      $item->set($k, $v);
  }
  return $item;
}

=head2 add_item

 Title   : add_item
 Usage   : $item = $doc->add_item("Gene");
 Function: return a new Item object of the given type,
           while storing it in an internal record for writing
           out later
 Args    : the classname of the new Item
           Any attributes for the item

=cut

sub add_item {
    my $self = shift;
    my $item = $self->make_item(@_);

    push @{$self->{unwritten_items}}, $item;
    if ($self->{auto_write}) {
        $self->write();
    }
    return $item;
}

1;
