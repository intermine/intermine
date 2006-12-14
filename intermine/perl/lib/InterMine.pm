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
        if ($field->field_type() eq 'reference') {
          push @columns, $field->field_name() . 'id', {type => 'int'};
          my $referenced_type_name = $field->referenced_type_name();
          $referenced_type_name =~ s/.*\.(.*)/InterMine::$1/;
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
#           if ($field->is_many_to_one()) {
            
#           } else {
#             _make_mapping_table($field)
#           }
        }
      }
    }

    $setup_args{table} = $class;
    $setup_args{columns} = \@columns;
    $setup_args{foreign_keys} = \@foreign_keys;

    my $lc_class = lc $class;

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

sub _make_mapping_table
{
  my $class = shift;
  my $field = shift;
  my $reverse_field = shift;
  my $field_name = $field->field_name();
  my $reverse_field_name = $reverse_field->field_name();

  eval <<"EOF"
package InterMine::$class\::

use base 'My::DB::Object';

__PACKAGE__->meta->setup
(
 table   => 'product_color_map',
 columns =>
 [
  product_id => { type => 'int', not_null => 1 },
  color_id   => { type => 'int', not_null => 1 },
 ],

 primary_key_columns => [ 'product_id', 'color_id' ],

 foreign_keys =>
 [
  product =>
  {
   class       => 'Product',
   key_columns => { product_id => 'id' },
  },

  color =>
  {
   class       => 'Color',
   key_columns => { color_id => 'id' },
  },
 ],
);
EOF

}

1;
