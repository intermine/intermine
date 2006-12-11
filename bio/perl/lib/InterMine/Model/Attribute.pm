package InterMine::Model::Attribute;

use strict;
use vars qw(@ISA);
use InterMine::Model::Field;

@ISA = qw(InterMine::Model::Field);

sub attribute_type
{
  my $self = shift;
  return $self->{type};
}

1;
