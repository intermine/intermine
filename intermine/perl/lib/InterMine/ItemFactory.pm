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
  my $self = { id_counter => 0, %opts, name_space => $model->name_space() };

  bless $self, $class;
  return $self;
}

sub make_item
{
  my $self = shift;
  my %args = @_;
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
