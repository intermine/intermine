package Webservice::InterMine;

use strict;
use warnings;
use Carp;

our $VERSION = "0.9705";

=head1 NAME

Webservice::InterMine - modules for interacting with InterMine datawarehouse webservices

=head1 SYNOPSIS

    use Webservice::InterMine;

    my $service  = Webservice::InterMine->new_service($url, $user, $pass);
    my $template = $service->template($name);
    my $results  = $template->results_with(valueA => 'x', valueB => 'y');

  OR

    use Webservice::InterMine 'flymine', 'some-username', 'some-password';

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

=head1 INSTALLATION AND DEPENDENCIES

This package can be installed using the following commands (Module::Build >= 0.36 
is required):

  perl Build.PL
  ./Build test
  sudo ./Build install

If any runtime dependencies are missing, you can use the following command to install them:

  sudo ./Build installdeps

=head2 IMPORT STATEMENTS

In the example above the modules are imported with the following statements:

    use Webservice::InterMine; 

and

    use Webservice::InterMine 'flymine', 'some-username', 'some-password';
  
Calling C<use Webservice::InterMine> without any parameters simply
means you need to either specify the webservice url on every call, or
call the methods on a service directly.

If you call C<use Webservice::InterMine $mine, [$user, $pass]>, a default service will be set,
meaning method calls will not require the webservice url. Unless you are
intending to access multiple services, this form is recommended.

=head1 METHODS

=cut

use Webservice::InterMine::Service;

our $CLEAN_UP = 1;

use constant CONFIG_FILE => $ENV{HOME} . '/.intermine-webservice.config';

my $service_url;

my %services;
my %pass_for;
my %user_for;

sub import {
    my $class = shift;
    my ( $url, $user, $pass ) = @_;
    if ($url) {
        $service_url = $url;
        return $class->get_service( $url, $user, $pass );
    }
}

=head2 new_query( [from => \@service_args], [%query_args] )

returns a new query object for you to fill in with constraints before
being run to get its results. 

Parameters:

=over

=item * from => [url, user, pass]

An array ref of arguments to pass to get_service. This information
can be used to specify a different service than the one named on import, or
to specify one when none was named on import. 

=item * %query_args

Key value pairs of arguments to pass to the query constructor in
L<Webservice::InterMine::Service>. This methid serves as sugar for the
factory method in that class.

=back

Please see L<Webservice::InterMine::Query>, L<Webservice::InterMine::Service>.

=cut

sub new_query {
    my $class = shift;
    my %args = @_;
    my $service_args = delete($args{from}) || [];
    return $class->get_service(@$service_args)->new_query(%args);
}

=head2 new_list( %list_args, [from => \@service_args] )

Creates a new list with the content specified by the list arguments. The
C<content> key-word parameter will always be required. For a full 
specification of creating lists, see: L<Webservice::InterMine::Service>.

Parameters:

=over

=item * from => [url, user, pass]

An array ref of arguments to pass to get_service. This information
can be used to specify a different service than the one named on import, or
to specify one when none was named on import. 

=item * %list_args

Key value pairs of arguments to pass to the list constructor in
L<Webservice::InterMine::Service>. This methid serves as sugar for the
factory method in that class.

=back

=cut

sub new_list {
    my $class = shift;
    my %args = @_;
    my $service_args = delete($args{from}) || [];
    return $class->get_service(@$service_args)->new_list(%args);
}

=head2 get_list( $list_name, [from => \@service_args] )

Get the list of the given name from the default service, or the given service if 
details are supplied.

=cut

sub get_list {
    my $class = shift;
    my $list_name = shift;
    my %args = @_;
    my $service_args = delete($args{from}) || [];
    return $class->get_service(@$service_args)->list($list_name);
}

=head2 load_query([\@service_args], source_file|source_string => $source, %opts )

Returns a query object based on xml you have previously saved,
either as a string or as a file. For a file pass:

  load_query(source_file => $file);

For a string: 

  load_query(source_string => $string);

If you want a specific service, call it thus:

  load_query(from => [$name, $user, $pass], source_string => $string);

OR: 

  load_query(from => [$name, $user, $pass], source_string => $string);

Please see L<Webservice::InterMine::Query::Saved>

=cut

sub load_query {
    my $class = shift;
    my %args = @_;
    my $service_args = delete($args{from}) || [];
    return $class->get_service(@$service_args)->new_from_xml(%args);
}

=head2 template( $name, [from => \@service_args], [%opts] )

returns the named template (if it exists - if not it returns undef).
If you pass a url, it returns the named template from the specified webservice.

Please see L<Webservice::InterMine::Query::Template>

=cut

sub template {
    my $class        = shift;
    my $name         = shift;
    my %args         = @_;
    my $service_args = delete($args{from}) || [];
    return $class->get_service(@$service_args)->template( $name, @_ );
}

=head2 saved_query( $name, [from => \@service_args], %options ) B<NOT IMPLEMENTED YET>

returns the named saved_query (if it exists - if not it returns undef).
If you pass a url, it returns the named query from the specified webservice.

This method requires you to have provided a username and password to the
webservice for authentication.

Please see L<Webservice::InterMine::Query::Saved>

=cut

sub saved_query {
    my $class = shift;
    my $name         = shift;
    my %args         = @_;
    my $service_args = delete($args{from}) || [];
    return $class->get_service(@_)->saved_query($name, %args);
}

=head2 get_service( $url, $user, $pass )

returns a webservice object, which is used to construct
queries and fetch templates and saved queries. If a url is
passed, the webservice for that url is returned, otherwise the
service for the url given to C<use> is returned.

If a service for a url has been created previously, that one is returned,
even if different login details are provided.

Please see: L<Webservice::InterMine::Service>

=cut

sub get_service {
    my $class = shift;

    my $url = shift || $service_url;
    croak "No url provided - either directly or on 'use'"
      unless $url;
    if ( $services{$url} ) {
        return $services{$url};
    }

    my ( $user, $pass ) = @_;
    unless ($user and $pass) {
        ($user, $pass) = get_saved_user_info($url);
    }

    if ( $user and $pass ) {
        $user_for{$url} = $user;
        $pass_for{$url} = $pass;
    }
    my $service = Webservice::InterMine::Service->new( $url, $user, $pass );
    $services{$url} = $service;
    return $service;
}

=head2 get_saved_user_info($mine_name/$mine_url)

Returns saved user name and passwords from the webservice config file
if it exists. This file should be located at ~/.intermine-webservices.config,
and should have the following format:

  flymine                        user@somewhere.edu password
  metabolicmine                  user@somewhere.edu another-password
  http://yeastmine.org/yeastmine user@somewhere.org A v3rj d1EE!kvlt 0n3

ie., whitespace separated fields. The password may contain whitespace characters, but no
new-lines. The mine-name/url needs to identical to the name used to request the service.

=cut

sub get_saved_user_info {
    my $key = shift;
    if (-f CONFIG_FILE) {
        open(my $in, '<', CONFIG_FILE) or confess "$!";
        while (<$in>) {
            my ($url, $user, $pass) = split(/\s+/, $_, 3);
            return($user, $pass) if ($url eq $key);
        }
        close $in or confess "$!";
    }
    return;
}

=head2 clean_temp_lists()

Deletes all automatically created anonymous lists. Any
renamed lists will be spared the clean-up. This method is called
on system exit, unless the variable $Webservice::InterMine::CLEAN_UP is 
set to a false value.

=cut

sub clean_temp_lists() {
    my $self = shift;
    for my $service (values %services) {
        $service->delete_temp_lists() if $service->_has_lists;
    }
}

=head2 get_{minename}(user, pass)

An unknown method preceded with 'get_' will be interpreted
as a mine name, and an attempt will be made to return a service with
that name.

=cut

sub AUTOLOAD {
    my $self = shift;
    my $method = our $AUTOLOAD;
    my @args = @_;
    $method =~ s/.*:://;
    my ($get, $service_name) = split(/_/, $method, 2);
    if ($get and $service_name and $get eq 'get') {
        return $self->get_service($service_name, @_);
    }
    confess "no method named $method in " . __PACKAGE__;
}

END {
    if ($CLEAN_UP) {
        __PACKAGE__->clean_temp_lists();
    }
}

=head1 SEE ALSO

=over 4

=item * L<Webservice::InterMine::Cookbook> for a guide on how to use these modules.

=item * L<Webservice::InterMine::Query>

=item * L<Webservice::InterMine::Service>

=item * L<Webservice::InterMine::List>

=item * L<Webservice::InterMine::Query::Template>

=item * L<Webservice::InterMine::Query::Saved>

=item * L<Webservice::InterMine::Bio> Biologically specific roles.

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
