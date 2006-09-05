package InterMine::Model::Field;

use strict;

sub new
{
  my $class = shift;
  my %opts = @_;
  my $self = {%opts};
  bless $self, $class;
  return $self;
}

sub name
{
  my $self = shift;
  return $self->{name};
}

sub get_field_type
{
  my $self = shift;
  return lc (((ref $self) =~ /.*::(.*)/)[0]);
}

1;
