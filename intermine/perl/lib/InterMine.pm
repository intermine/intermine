package InterMine;

use warnings;
use strict;

=head1 NAME

InterMine - perl code for accessing an InterMine data warehouse

=head1 VERSION

Version 0.01

=cut

our $VERSION = '0.01';

=head1 SYNOPSIS

=head1 EXPORT

=head1 AUTHOR

FlyMine, C<< <perl@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<< <perl@flymine.org> >>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=item * AnnoCPAN: Annotated CPAN documentation

L<http://annocpan.org/dist/InterMine>

=item * CPAN Ratings

L<http://cpanratings.perl.org/d/InterMine>

=item * RT: CPAN's request tracker

L<http://rt.cpan.org/NoAuth/Bugs.html?Dist=InterMine>

=item * Search CPAN

L<http://search.cpan.org/dist/InterMine>

=back

=head1 ACKNOWLEDGEMENTS

=head1 COPYRIGHT & LICENSE

Copyright 2006 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut

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

  if (grep /__ALL__/, @classes) {
    @classes = map {($_->name() =~ m/.*\.(.*)/)} get_model()->get_all_classdescriptors();
  }

  my @classes_to_import = ();

  for my $class (@classes) {
    my $cd = get_model()->get_classdescriptor_by_name($class);

    my %setup_args = (primary_key_columns => [ 'id' ]);
    my @columns = (id => { type => 'int', primary_key => 1, not_null => 1 });
    my @relationships = ();
    my @foreign_keys = ();

    for my $field ($cd->fields()) {

      if ($field->field_type() eq 'attribute') {
        push @columns, $field->field_name(), {type => $field->attribute_type()}
      } else {
        my $referenced_type_name = $field->referenced_type_name();
        $referenced_type_name =~ s/.*\.(.*)/InterMine::$1/;
        if ($field->field_type() eq 'reference') {
          push @columns, $field->field_name() . 'id', {type => 'int'};
          my $foreign_key_settings =
            {
             class => $referenced_type_name,
             key_columns => {
                             $field->field_name() . 'id',
                             'id'
                            }
            };
          push @foreign_keys, $field->field_name(), $foreign_key_settings;
        } else {
           if ($field->is_one_to_many()) {
             my $reverse_reference_field_name =
               $field->reverse_reference()->field_name();
             my $relationship_settings =
             {
               type       => 'one to many',
               class      => $referenced_type_name,
               column_map => { id => $reverse_reference_field_name . 'id' },
             };
             push @relationships, $field->field_name(), $relationship_settings;
           } else {
             if ($field->is_many_to_many()) {
               my $reverse_reference_field_name =
                 $field->reverse_reference()->field_name();
               my $map_class = _make_mapping_table($field);
               my $relationship_settings =
               {
                type      => 'many to many',
                map_class => $map_class,
                map_from  => lc $field->field_name() . "_key",
                map_to    => lc $reverse_reference_field_name . "_key",
               };
               push @relationships, $field->field_name(), $relationship_settings;
             } else {
               warn "unimplemented: ", $field->field_name(), "\n";
             }
           }
        }
      }
    }

    $setup_args{table} = $class;
    $setup_args{columns} = \@columns;
    $setup_args{foreign_keys} = \@foreign_keys;
    $setup_args{relationships} = \@relationships;

    my $lc_class = lc $class;

    $setup_args{table} = $class;

    eval <<"EOF";
package InterMine::$class;
use base 'InterMine::DB::Object';
__PACKAGE__->meta->setup(%setup_args);
1;

package InterMine::$class\::Manager;
use base 'Rose::DB::Object::Manager';
sub object_class { 'InterMine::$class' }
__PACKAGE__->make_manager_methods('${lc_class}s');
1;

EOF

    die $@ if $@;
  }
}

my %postgres_keywords = (
                         translation => 1
);

sub _table_for_class
{
  my $class_name = lc shift;
  warn "class_name: $class_name\n";

  if (exists $postgres_keywords{$class_name}) {
    return "intermine_$class_name";
  } else {
    return $class_name;
  }
}


sub _make_mapping_table
{
  my $field = shift;
  my $reverse_field = $field->reverse_reference();
  my $field_name = $field->field_name();
  my $reverse_field_name = $reverse_field->field_name();

  my $lc_field_name = lc $field_name;
  my $lc_reverse_field_name = lc $reverse_field_name;

  my $field_class_name = $reverse_field->field_class()->unqualified_name();
  my $reverse_field_class_name = $field->field_class()->unqualified_name();

  my $map_class = 'InterMine::' . ucfirst $field_name . ucfirst $reverse_field_name . 'Map';

  my $eval_string = <<"EOF";
package $map_class;

use base 'InterMine::DB::Object';

__PACKAGE__->meta->setup
(
 table   => '${lc_reverse_field_name}${lc_field_name}',
 columns =>
 [
  ${lc_field_name} => { type => 'int', not_null => 1 },
  ${lc_reverse_field_name} => { type => 'int', not_null => 1 },
 ],

 primary_key_columns => [ '${lc_field_name}', '${lc_reverse_field_name}' ],

 foreign_keys =>
 [
  ${lc_field_name}_key =>
  {
   class       => 'InterMine::$reverse_field_class_name',
   key_columns => { ${lc_field_name} => 'id' },
  },

  ${lc_reverse_field_name}_key =>
  {
   class       => 'InterMine::$field_class_name',
   key_columns => { ${lc_reverse_field_name} => 'id' },
  },
 ],
);
EOF

  eval $eval_string;

  return $map_class;
}

1;
