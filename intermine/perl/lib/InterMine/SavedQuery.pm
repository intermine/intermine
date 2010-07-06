package InterMine::SavedQuery;

=head1 NAME

InterMine::SavedQuery - the representation of an InterMine model

=head1 SYNOPSIS

  use InterMine::SavedQuery;

  my $query = new InterMine::SavedQuery(file => $saved_query_file,
                                        model => $model);

  ...

=head1 DESCRIPTION

The class is the Perl representation of an InterMine saved query.

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::SavedQuery;

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008,2009,2010 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use strict;
use warnings;
use Carp;

use base qw(InterMine::Template);

sub new {
    my ($class, @args) = @_;
    return $class->SUPER::new(@args, type => 'saved-query');
}

sub _validate_has_views {
    # Saved queries are valid, even if they have no view, 
    # as they are meant to be loaded and edited
    return 1;
}

1;
