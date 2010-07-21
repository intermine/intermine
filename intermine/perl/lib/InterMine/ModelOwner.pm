package InterMine::ModelOwner;

use strict;
use warnings;
use Carp;

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

use Scalar::Util qw(weaken);

sub model {
    my $self = shift;
    my $model = shift;
    if ($model) {
	croak "Invalid model"
	    unless (UNIVERSAL::can($model, 'isa') 
		    and $model->isa('InterMine::Model')
	    );
	$self->{model} = $model;
	weaken($self->{model});
    }
    return $self->{model};
}

1;
    
