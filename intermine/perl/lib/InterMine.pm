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
