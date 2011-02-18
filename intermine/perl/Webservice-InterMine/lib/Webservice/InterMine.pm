package Webservice::InterMine;

use strict;
use warnings;
use Carp;

our $VERSION = "0.9600";

=head1 NAME

Webservice::InterMine - modules for interacting with InterMine datawarehouse webservices

=head1 SYNOPSIS

    use Webservice::InterMine;

    my $service  = Webservice::InterMine->new_service($url);
    my $template = $service->template($name);
    my $results  = $template->results_with(valueA => 'x', valueB => 'y');

  OR

    use Webservice::InterMine 'www.flymine.org';

    my $query    = Webservice::InterMine->new_query;
    $query->add_view(@views);
    $query->add_constraint(
                    path  => $path,
                    op    => $op,
                    value => $value,
                    );
    my $results  = $query->results;


=head1 DESCRIPTION

This distribution is the client interface to any implementation of
the InterMine Datawarehousing WebService (www.intermine.org).
Primarily used for biological genomic databases, the webservice
allows the user to easily write and execute structured queries.

This module allows you to interact with one or more webservices
by providing a url to an InterMine implementation.

=head2 Usage

You can call C<use Webservice::InterMine> without any parameters, which simply
means you need to either specify the webservice url on every call, or
call the methods on a service directly.

If you call C<use Webservice::InterMine $url>, a default service will be set,
meaning method calls will not require the webservice url. Unless you are
intending to access multiple services, the latter form is recommended.

=head1 METHODS

=cut

use Webservice::InterMine::Service;

my $service_url;

my %services;
my %pass_for;
my %user_for;

sub import {
    my $class = shift;
    my ($url, $user, $pass) = @_;
    if ($url) {
        $service_url = $url;
        return $class->get_service($url, $user, $pass);
    }
};

=head2 new_query( [$url] )

returns a new query object for you to fill in with constraints before
being run to get its results. If you pass a url, it constructs a query
for the specified webservice.

Please see L<Webservice::InterMine::Query>

=cut

sub new_query {
    my $class = shift;
    my %args  = @_;
    my $roles = delete $args{with};
    return $class->get_service(%args)->new_query(with => $roles);
}


=head2 template( $name, [$url] )

returns the named template (if it exists - if not it returns undef).
If you pass a url, it returns the named template from the specified webservice.

Please see L<Webservice::InterMine::Query::Template>

=cut

sub template {
    my $class = shift;
    my $name  = shift;
    my %args  = @_;
    my $roles = delete $args{with};
    return $class->get_service(%args)->template($name, $roles);
}

=head2 saved_query( $name, [$url] ) B<NOT IMPLEMENTED YET>

returns the named saved_query (if it exists - if not it returns undef).
If you pass a url, it returns the named query from the specified webservice.

This method requires you to have provided a username and password to the
webservice for authentication.

Please see L<Webservice::InterMine::Query::Saved>

=cut

sub saved_query {
    my $class = shift;
    my $name  = shift;
    return $class->get_service(@_)->saved_query($name);
}

=head2 get_service( [$url, $user, $pass] )

returns a webservice object, which is used to construct
queries and fetch templates and saved queries. If a url is
passed, the webservice for that url is returned, otherwise the
service for the url given to C<use> is returned.

Please note: user and password based authentication has not yet
been implemented.

Please see: L<Webservice::InterMine::Service>

=cut

sub get_service {
    my $class = shift;
    my $url   = shift || $service_url;
    my ($user, $pass) = @_;
    croak "No url provided - either directly or on 'use'"
        unless $url;
    if ($services{$url}) {
        return $services{$url};
    } else {
        if ($user and $pass) {
            $user_for{$url} = $user;
            $pass_for{$url} = $pass;
        }
        my $service = Webservice::InterMine::Service->new($url, $user, $pass);
        $services{$url} = $service;
        return $service;
    }
}

=head1 SEE ALSO

=over 4

=item * L<Webservice::InterMine::Cookbook> for guide on how to use these modules.

=item * L<Webservice::InterMine::Query>

=item * L<Webservice::InterMine::Service>

=item * L<Webservice::InterMine::Query::Template>

=item * L<Webservice::InterMine::Query::Saved>

=back

=head1 AUTHOR

Alex Kalderimis C<< <dev@intermine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<dev@intermine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc Webservice::InterMine

You can also look for information at:

=over 4

=item * Webservice::InterMine

L<http://www.intermine.org>

=item * Documentation

L<http://www.intermine.org/perlapi>

=back

=head1 COPYRIGHT AND LICENSE

Copyright 2006 - 2010 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=cut

1;
