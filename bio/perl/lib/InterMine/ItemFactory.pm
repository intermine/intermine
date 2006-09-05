package InterMine::ItemFactory;

use strict;

use InterMine::Item;

sub new
{
  my $class = shift;
  my %opts = @_;

  if (!exists $opts{model}) {
    die "-model argument missing in ItemFactory constructor\n";
  }
  my $model = $opts{model};
  my $self = { id_counter => 0, %opts, namespace => $model->namespace() };

  bless $self, $class;
  return $self;
}

sub make_item
{
  my $self = shift;
  my $class = shift;
  $self->{id_counter}++;
  return new InterMine::Item(classname => $self->{model}->namespace() . "$class",
                             model => $self->{model},
                             id => $self->{id_counter});
}

1;
