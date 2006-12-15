package InterMine::Model::Field;

use strict;

my %type_map = (
                'java.lang.String' => 'text',
                'java.lang.Boolean' => 'boolean',
                'java.lang.Float' => 'float',
                'java.lang.Double' => 'float',
                'java.lang.Integer' => 'int'
               );

sub new
{
  my $class = shift;
  my %opts = @_;
  my $self = {%opts};

  if (exists $opts{type}) {
    my $type = $opts{type};
    if (exists $type_map{$type}) {
      $self->{type} = $type_map{$type};
    }
  }

  bless $self, $class;
  return $self;
}

sub field_name
{
  my $self = shift;
  return $self->{name};
}

sub field_type
{
  my $self = shift;
  return lc (((ref $self) =~ /.*::(.*)/)[0]);
}

# returns the ClassDescriptor of the (base) class that defines this field
sub field_class
{
  my $self = shift;
  if (@_ > 0) {
    $self->{field_class} = shift;
  } else {
    return $self->{field_class};
  }
}

1;
