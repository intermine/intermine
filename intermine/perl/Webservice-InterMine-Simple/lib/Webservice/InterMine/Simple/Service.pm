package Webservice::InterMine::Simple::Service;

use strict;
use Webservice::InterMine::Simple::Query;
use Webservice::InterMine::Simple::Template;
use LWP;
use MIME::Base64;

use constant USER_AGENT => 'WebserviceInterMinePerlAPIClient';

=head1 NAME

Webservice::InterMine::Simple::Service

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

This is a basic representation of a connection to an InterMine web-service. It has 
some facilities for handling simple queries and templates.

=head1 METHODS

=head2 new - Construct a new service.

Parameters:

=over

=item root => $url 

The root url of the webservice.

=item token => $token

The authorisation token of the user (optional)

=item user => $username

The login name of the user (optional - requires a password)

=item pass => $password

The password of the user.

=back

=cut

sub new {
    my $class = shift;
    my $self = {@_};
    my $ua = LWP::UserAgent->new;
    $ua->env_proxy;
    $ua->agent(USER_AGENT);
    if ($self->{user} and $self->{pass}) {
        my $auth_string = join(':', $self->{user}, $self->{pass});
        $ua->default_header( Authorization => encode_base64($auth_string) );
    }
    $self->{ua} = $ua;
    return bless $self, $class;
}

=head2 new_from_xml 

Construct a new query from an XML serialisation.

Parameters:

=over 

=item * source_file => $filename

=item * source_string => $string

=back

Only one source argument is required.

=cut

sub new_from_xml {
    my $self = shift;
    my %args = @_;
    $args{service} = $self;
    return Webservice::InterMine::Simple::Query->new_from_xml(%args);
}

=head2 new_query

Construct a new blank query.

=cut

sub new_query {
    my $self = shift;
    my %args = @_;
    $args{service} = $self;
    return Webservice::InterMine::Simple::Query->new(%args);
}

=head2 template($name)

Construct a new template object to retrieve results from the template
with the given name.

=cut

sub template {
    my $self = shift;
    my $name = shift;
    my %args = (
        name => $name,
        service => $self,
    );
    return Webservice::InterMine::Simple::Template->new(%args);
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

