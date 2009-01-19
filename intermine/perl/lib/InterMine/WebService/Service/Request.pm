package InterMine::WebService::Service::Request;

=head1 NAME

InterMine::WebService::Service:: - Service for making InterMine
queries using the web service

=head1 SYNOPSIS

  my $factory = new InterMine::ItemFactory(serviceroot => $root, "QueryClient");

  my $query_service = $factory->get_query_service();

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::WebService::Service::QueryService

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008,2009 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use strict;


