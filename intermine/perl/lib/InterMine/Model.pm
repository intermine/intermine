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

  $self->_fix_class_descriptors();

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
    my $model = $self->{model};
    if ($args->{Name} eq "class") {
      my @extends = ();
      if (exists $args->{Attributes}{extends}) {
        @extends = split /\s+/, $args->{Attributes}{extends};
        map { s/.*\.(.*)/$1/ } @extends;
      }
      $self->{current_class} =
        new InterMine::Model::ClassDescriptor(model => $model,
                                              name => $nameattr, extends => [@extends]);
    } else {
      my $field;
      if ($args->{Name} eq "attribute") {
        my $type = $args->{Attributes}{type};
        $field = InterMine::Model::Attribute->new(name => $nameattr,
                                                  type => $type,
                                                  model => $model);
      } else {
        my $referenced_type = $args->{Attributes}{'referenced-type'};
        my $reverse_reference = $args->{Attributes}{'reverse-reference'};
        if ($args->{Name} eq "reference") {
          $field = InterMine::Model::Reference->new(name => $nameattr,
                                                    referenced_type_name =>
                                                      $referenced_type,
                                                    reverse_reference_name =>
                                                      $reverse_reference,
                                                    model => $model);
        } else {
          if ($args->{Name} eq "collection") {
            $field = InterMine::Model::Collection->new(name => $nameattr,
                                                       referenced_type_name =>
                                                         $referenced_type,
                                                       reverse_reference_name =>
                                                         $reverse_reference,
                                                       model => $model);
          } else {
            die "unexpected element: ", $args->{Name}, "\n";
          }
        }
      }

      $field->field_class($self->{current_class});

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

  my $package_name = _namespace_to_package_name($handler->{namespace});

  for my $class (@{$self->{classes}}) {
    my $classname = $class->name();
    $self->{class_hash}{$classname} = $class;
  }

  $self->{name_space} = $handler->{namespace};
  $self->{package_name} = $package_name;
  $self->{model_name} = $handler->{modelname};
}

sub _namespace_to_package_name
{
  my $out_name = shift;

  if ($out_name =~ m@http://(?:www\.)(.*?)/(.*)?/(.*)?\#@) {
    my $domain = $1;
    my @domain_bits = split /\./, $domain;
    my $out_name = join '.', reverse @domain_bits;

    if (defined $2) {
      $out_name .= ".$2";
    }
    if (defined $3) {
      $out_name .= ".$3";
    }
    return $out_name;
  } else {
    die "cannot understand namespace: $out_name\n";
  }
}

# add fields from base classes to sub-classes so that $class_descriptor->fields()
# returns fields from base classes too
sub _fix_class_descriptors
{
  my $self = shift;

  while (my ($class_name, $cd) = each %{$self->{class_hash}}){
    my @fields = $self->_get_fields($cd);
    for my $field (@fields) {
      $cd->add_field($field);
    }
  }
}

sub _get_fields
{
  my $self = shift;
  my $cd = shift;

  my @fields = ();

  for my $field ($cd->fields()) {
    my $field_name = $field->field_name();
    push @fields, $field;
  }

  my @extends = $cd->extends();

  for my $extendee_name (@extends) {
    my $extendee = $self->get_classdescriptor_by_name($extendee_name);

    push @fields, $self->_get_fields($extendee);
  }

  return @fields;
}


sub get_classdescriptor_by_name
{
  my $self = shift;
  my $classname = shift;

  if (exists $self->{class_hash}{$classname}) {
    return $self->{class_hash}{$classname};
  } else {
    my $full_classname = $self->{package_name} . '.' . $classname;
    return $self->{class_hash}{$full_classname};
  }
}

sub get_all_classdescriptors
{
  my $self = shift;
  return values %{$self->{class_hash}};
}


sub name_space
{
  my $self = shift;
  return $self->{name_space};
}

sub model_name
{
  my $self = shift;
  return $self->{model_name};
}

1;
