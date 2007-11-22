package InterMine::ItemFactory;

=head1 NAME

InterMine::ItemFactory - factory for creating Item objects that match a given 
Model

=head1 SYNOPSIS

  use InterMine::ItemFactory;

  my $model = new InterMine::Model(file => $model_file);

  my $item_factory = new InterMine::ItemFactory(model => $model);

  

=head1 DESCRIPTION

C<compare()> compares two files or arrays of strings and returns a MatchMap
object holding the results.

=cut

=head1 FUNCTIONS

=cut

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
  my $self = { id_counter => 0, %opts, name_space => $model->name_space() };

  bless $self, $class;
  return $self;
}

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
