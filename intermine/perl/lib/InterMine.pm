package InterMine;

use InterMine::Model;

our $model_file;
our $model;

sub get_model {
  die "model_file not set\n" unless defined $model_file;

  if (defined $model) {
    return $model;
  } else {
    $model = new InterMine::Model(file => $model_file);
  }
}

sub import {
  my $pkg = shift;

  my @classes = @_;

  for my $class (@classes) {
    my $cd = get_model()->get_classdescriptor_by_name($class);

    my %setup_args = ();
    my @fields = (id => { type => 'int', primary_key => 1 });

    for my $field ($cd->fields()) {
      if ($field->field_type() eq 'attribute') {
        push @fields, $field->field_name(), {type => $field->attribute_type()}
      }
    }

    $setup_args{table} = $class;
    $setup_args{columns} = \@fields;

    eval "
package InterMine::$class;
use base 'InterMine::DB::Object';
__PACKAGE__->meta->setup(%setup_args);
1;
";

    die $@ if $@;

  }
}

1;
