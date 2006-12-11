package InterMine::Model;

use strict;

sub new
{
  my $class = shift;
  my %opts = @_;
  my $self = {%opts};

  if (!defined $opts{file}) {
    die "$class\::new() needs a file argument\n";
  }

  $self->{class_hash} = {};

  bless $self, $class;
  $self->_process($opts{file});

  return $self;
}

use InterMine::Model::Attribute;
use InterMine::Model::Reference;
use InterMine::Model::Collection;
use InterMine::Model::ClassDescriptor;

package InterMine::Model::Handler;

use vars qw{ $AUTOLOAD };

sub new {
  my $type = shift;
  my $self = ( $#_ == 0 ) ? shift : { @_ };

  return bless $self, $type;
}

sub start_element
{
  my $self = shift;
  my $args = shift;

  $self->{current_element} = $args->{Name};

  my $nameattr = $args->{Attributes}{name};

  if ($args->{Name} eq "model") {
    $self->{modelname} = $nameattr;
    $self->{namespace} = $args->{Attributes}{namespace};
  } else {
    if ($args->{Name} eq "class") {
      my @extends = ();
      if (exists $args->{Attributes}{extends}) {
        @extends = split /\s+/, $args->{Attributes}{extends};
        map { s/.*\.(.*)/$1/ } @extends;
      }
      $self->{current_class} =
        new InterMine::Model::ClassDescriptor(model => $self->{model},
                                              name => $nameattr, extends => [@extends]);
    } else {
      my $field;
      if ($args->{Name} eq "attribute") {
        my $type = $args->{Attributes}{type};
        $field = InterMine::Model::Attribute->new(name => $nameattr,
                                                  type => $type);
      } else {
        if ($args->{Name} eq "reference") {
          $field = InterMine::Model::Reference->new(name => $nameattr);
        } else {
          if ($args->{Name} eq "collection") {
            $field = InterMine::Model::Collection->new(name => $nameattr);
          } else {
            die "unexpected element: ", $args->{Name}, "\n";
          }
        }
      }

      $self->{current_class}->add_field($field);
    }
  }
}

sub end_element {
  my $self = shift;
  my $args = shift;
  #  print "end_element: ", $args->{Name}, "\n";
  if ($args->{Name} eq 'class') {
    push @{$self->{classes}}, $self->{current_class};
    $self->{current_class} = undef;
  }
}

1;

package InterMine::Model;

use XML::Parser::PerlSAX;

sub _process
{
  my $self = shift;
  my $file = shift;
  my $handler = new InterMine::Model::Handler(model => $self);
  my $parser = XML::Parser::PerlSAX->new(Handler => $handler);

  $parser->parse(Source => { SystemId => $file });

  $self->{classes} = $handler->{classes};

  for my $class (@{$self->{classes}}) {
    my $classname = $handler->{namespace} . ($class->name() =~ /.*\.(.*)/)[0];
    $self->{class_hash}{$classname} = $class;
  }

  $self->{namespace} = $handler->{namespace};
  $self->{modelname} = $handler->{modelname};
}

sub get_classdescriptor_by_name
{
  my $self = shift;
  my $classname = shift;
  if (exists $self->{class_hash}{$classname}) {
    return $self->{class_hash}{$classname};
  } else {
    return $self->{class_hash}{$self->{namespace} . $classname}
  }
}

sub namespace
{
  my $self = shift;
  return $self->{namespace};
}

sub modelname
{
  my $self = shift;
  return $self->{modelname};
}

1;
