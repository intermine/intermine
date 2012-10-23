package Webservice::InterMine;

use strict;
use warnings;
use Carp;

our $VERSION = "1.0000";

=head1 NAME

Webservice::InterMine - modules for interacting with InterMine datawarehouse webservices

=head1 SYNOPSIS

    use Webservice::InterMine;

    my $service  = get_service($url, $user, $pass);
    my $template = $service->template($name);
    my $results  = $template->results_with(valueA => 'x', valueB => 'y');

OR:

    use Webservice::InterMine 'flymine', 'SOMETOKEN';

    my $results = new_query(class => 'Gene')
                    ->select('symbol', 'primaryIdentifier', 'pathways.*')
                    ->where(symbol => [qw/H bib eve zen/])
                    ->all();

OR:

    use Webservice::InterMine 'flymine', 'SOMETOKEN', ':no-import';

    my $results = Webservice::InterMine->new_query(class => 'Gene')
                    ->select('symbol', 'primaryIdentifier', 'pathways.*')
                    ->where(symbol => [qw/H bib eve zen/])
                    ->all();

OR: 

    use WebService::InterMine 'flymine';

    my $query = load_query(source_file => "my_query.xml");

    while (my $result = <$query>) {
        process($result);
    }

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

You must import the module with "use" to utilise its functionality. Additional
parameters can be supplied at import to specify a default service.

Simply import the module:

    use Webservice::InterMine; 

Import the module, and specify the default service as FlyMine (the url will be looked
up at the central mine registry L<http://www.intermine.org/registry>.

    use Webservice::InterMine 'flymine';

The same, only with the full url, eliminating the need for an extra lookup:

    use Webservice::InterMine 'www.flymine.org/query';

The same, but with authentication supplied by an API access token:

    use Webservice::InterMine 'flymine', 'SOMETOKEN';

The same, but with authentication supplied username and password credentials: (deprecated)

    use Webservice::InterMine 'flymine', 'username', 'password';

The same, with explicit, named parameters:

    use Webservice::InterMine root => 'flymine', user => 'username', pass => 'password';
  
Calling C<use Webservice::InterMine> without any parameters simply
means you need to either specify the webservice url on every call, or
call the methods on a service directly.

If you pass parameters to import, a default service will be set
meaning method calls will not require the webservice url. Unless you are
intending to access multiple services, this form is recommended.

If you do not wish to import the standard functions into your namespace, instead
choosing to call them as class methods, but still wish to set a default service, 
you can use the following syntax:

    use Webservice::InterMine "flymine", "TOKEN", ':no-import';

=head2 Using Queries

The main reason to access the InterMine webservices is to query for data. 
The most flexible way to do this is by using Queries:

    my $query = resultset("Gene")->select("*", "proteins.*")->where(symbol => [qw/h r eve zen bib/]);

    while (my $gene = <$query>) {
        print $gene->symbol, map {$_->name} $gene->proteins, "\n";
    }

As you can see, queries can be concisely defined, and their results easily 
accessed. You can process a query's results all at once, or using iteration, as in
the example above. Iteration is less memory hungry, as only the current row of 
data needs to be held in memory at once, which can be important when dealing
with large data sets.

=head2 Using Templates

Templates are predefined queries that make running queries easier, as you don't
have to define the logic yourself. Every mine comes with many predefined
searches, and you just have to enter a few parameters:

    my $results = $service->template("Gene_Proteins")->results_with(valueA => "eve");

These templates are simple to use, and they have all the power of full queries 
as well. You can process results just as powerfully with them.

=head2 Using Lists

Lists are saved result sets in a webservice you can access, create, modify and delete
(provided you are logged in):

    my $service = Webservice::InterMine->get_service("flymine", "MYTOKEN");
    my $list = $service->list("MyAwesomeList");
    my $new_list = $service->new_list(content => "some_file.text", type => "Gene");
    my $intersection = $list & $new_list;
    $intersection->rename("GenesInCommon");
    $new_list->delete; # Not actually necessary - it had no name so would have been automatically deleted anyway.

Lists enable you to collect and manage results and lists of items of interest to you.
You can use them in queries, and display their contents, as well as perforing
set-logic operations on them. When you are done, you can view the lists in the webservice, 
where you can use the analysis widgets to assess the data you have processed.

=head1 IMPORTS

The following functions are imported into the callers namespace. This can be prevented by "require"-ing 
the module instead.

=over

=item * get_service

=item * new_query

=item * resultset

=item * get_template
 
=item * get_list

=item * new_list

=item * load_query

=back

=head1 METHODS

=cut

use Webservice::InterMine::Service;
use base "Exporter";

our $CLEAN_UP = 1;

use constant CONFIG_FILE => $ENV{HOME} . '/.intermine-webservice.config';

my $service_url;

my %services;
my %pass_for;
my %user_for;

our @EXPORT_OK = ("get_service", "new_query", "get_template", "get_list", "new_list", "resultset", "load_query");

sub import {
    my $class = shift;
    my @args = grep {$_ ne ':no-import'} @_;
    if (@args) {
        my $service = $class->get_service(@args);
        $service_url = $service->root;
    }
    if (@args == @_) { # ie. the ':no-import' flag was not passed.
        Webservice::InterMine->export_to_level(1, $class, @EXPORT_OK);
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
    my $class = ($_[0] and $_[0] eq "Webservice::InterMine") ? shift : "Webservice::InterMine";
    my %args = @_;
    my $service_args = delete($args{from}) || [];
    return $class->get_service(@$service_args)->new_query(%args);
}

sub resultset {
    my $class = ($_[0] and $_[0] eq "Webservice::InterMine") ? shift : "Webservice::InterMine";
    my $root = shift;
    my %args = @_;
    $args{class} = $root;
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
    my $class = ($_[0] and $_[0] eq "Webservice::InterMine") ? shift : "Webservice::InterMine";
    my %args = @_;
    my $service_args = delete($args{from}) || [];
    return $class->get_service(@$service_args)->new_list(%args);
}

=head2 get_list( $list_name, [from => \@service_args] )

Get the list of the given name from the default service, or the given service if 
details are supplied.

=cut

sub get_list {
    my $class = ($_[0] and $_[0] eq "Webservice::InterMine") ? shift : "Webservice::InterMine";
    my $list_name = shift;
    my %args = @_;
    my $service_args = delete($args{from}) || [];
    return $class->get_service(@$service_args)->list($list_name);
}

=head2 load_query(source_file|source_string => $source, %opts, [from => [\@service_args]])

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
    my $class = ($_[0] and $_[0] eq "Webservice::InterMine") ? shift : "Webservice::InterMine";
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
    my $class = ($_[0] and $_[0] eq "Webservice::InterMine") ? shift : "Webservice::InterMine";
    my $name         = shift;
    my %args         = @_;
    my $service_args = delete($args{from}) || [];
    return $class->get_service(@$service_args)->template( $name, @_ );
}

=head2 get_template 

Alias for C<template>

=cut

sub get_template { goto &template }

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

=head2 get_service( $root, ($token | $user, $pass) )

returns a webservice object, which is used to construct
queries and fetch templates and saved queries. If a url is
passed, the webservice for that url is returned, otherwise the
service for the url given to C<use> is returned.

Get the default service defined at import:

  use Webservice::InterMine qw(http://www.intermine.org/intermine-demo SOMETOKEN);

  my $service =  InterMine::Webservice->get_service;

OR get a specific service, authenticated using an API access token:

  my $service = InterMine::Webservice->get_service($url, $token);

Fetch that same authenticated service later:

  my $service = InterMine::Webservice->get_service($url);

OR make the same call using a hash reference and named parameters:

  my $service = InterMine::Webservice->get_service({ 
    root => $url, token => $token
  });

Parameters:

=over 4

=item * $root - The base url for the service.

=item * $token - An API access key

=item * $username, $password - A user's login credentials

=back

InterMine webservices support two authentication mechanisms to provide
access to private data. The recommended method is to use API access
keys, which have the advantage of eliminating the need to transmit your 
login details. If compromised, an access key can be easily changed
or disabled without needing to change either the associated username or password.
However this facility is new, so for backwards compatibility, the older
mechanism for passing along username and password credentials has been retained.

If three arguments are supplied, they will be interpreted as C<($root, $user, $pass)>, 
but if two are supplied, they will be intepreted as C<($root, $token)>.

These arguments may also be passed in a keyword/hash style, either as a list
or a hash reference, in which case the full list will be passed to the 
L<Webservice::InterMine::Service::new> constructor.

If a service for a url has been created previously, that one is returned,
even if different login details are provided.

Please see: L<Webservice::InterMine::Service>

=cut

sub get_service {
    my $class = ($_[0] and $_[0] eq "Webservice::InterMine") ? shift : "Webservice::InterMine";

    my @args = (@_ == 1 and ref $_[0] eq 'HASH') ? %{$_[0]} : @_;
    my $url;

    # Try to interpret the arguments as a hash
    if (@args % 2 == 0) {
        my %hash = @args;
        $url = $hash{root};
    }

    # Prefer a keyword parameter, then take the first positional parameter,
    # then fallback to the default service url
    $url = $url || $_[0] || $service_url;
    croak "No url provided - either directly or on 'use'"
      unless $url;
    if ( $services{$url} ) {
        return $services{$url};
    }

    my $service = Webservice::InterMine::Service->new( @args );

    # Store service under both user supplied and expanded form
    $services{$url} = $service;
    $services{$service->root} = $service;
    return $service;
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



