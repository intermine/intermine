package Webservice::InterMine::Simple;

our $VERSION = "0.9800";

use strict;
use Webservice::InterMine::Simple::Service;
use Exporter 'import';

our @EXPORT_OK = ("get_service");
our @EXPORT = @EXPORT_OK;

=head1 NAME

Webservice::InterMine::Simple - A basic InterMine web-service client.

=head1 SYNOPSIS

  my $service = get_service("http://www.flymine.org/query/service");
  my $query = $service->new_query;

  $query->add_view("Organism.shortName", "Organism.taxonId");
  $query->add_constraint({path => "Organism.genus", op => "=", value => "Rattus"});

  @rows = $query2->results_table;
  for my $row (@rows) {
    print "name: $row->[0], id: $row->[1]\n";
  }

=head1 DESCRIPTION

This is a basic representation of a query. It can handle straight-forward
requests and result sets, but for anything more complicated, we recommend you look
as the more fully featured L<Webservice::InterMine>. This is especially true of 
large data sets, as this implementation has the potential to use lots of memory when 
receiving and processing results.

=head1 IMPORTED ROUTINES

=head2 get_service(@args)

Get a service instantiated with the given arguments.

Arguments:

=over 

=item * ($url) - The root-url to the webservice.

=item * ($url, $token) - The root-url, and an authorisation token.

=item * ($url, $username, $password) - The root-url, and a user's login information.

=back

This can also be called as a static method on the package:

  Webservice::InterMine::Simple->get_service(...);

=cut

sub get_service {
    if ($_[0] and $_[0] eq __PACKAGE__) {
    	my $self = shift;
    }
    if (@_ == 3) {
        my ($url, $user, $pass) = @_;
        return Webservice::InterMine::Simple::Service->new(
            root => $url,
            user => $user,
            pass => $pass,
        );
    } elsif (@_ == 2) {
        my ($url, $token) = @_;
        return Webservice::InterMine::Simple::Service->new(
            root => $url,
            token => $token,
        );
    } else {
        my ($url) = @_;
        return Webservice::InterMine::Simple::Service->new(
            root => $url,
        );
    }
}   

=head1 SEE ALSO

=over 4

=item * L<Webservice::InterMine> For a more powerful alternative

=back

=head1 AUTHOR

Alex Kalderimis C<< <dev@intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine::Simple

You can also look for information at:

=over 4

=item * InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/perlapi>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2011 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut

1;

