
=head1 NAME

InterMine::ModelOwner - methods for objects that have InterMine::Models

=head1 SYNOPSIS

  use InterMine::ModelOwner;

  my $self  = $class->new();
  my $model = new InterMine::Template(file => $template_file);
  $self->model = $model;
  ...

=head1 DESCRIPTION

Classes can inherit from this module to get methods for dealing with models

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::ModelOwner;

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008,2009 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 METHODS

=cut

package InterMine::Roles::ModelOwner;
use InterMine::TypeLibrary qw(Model);
use Moose::Role;

has model => (
    is => 'ro',
    isa => Model,
    required => 1,
    coerce => 1,
    lazy => 1,
# Invalid, but consumers that define their own defaults will get better results, otherwise it will need to be in the constructor
    default => sub {},
    handles => {
	model_name => 'model_name',
    },
   );

1;
