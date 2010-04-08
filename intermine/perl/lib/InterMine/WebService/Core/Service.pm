package InterMine::WebService::Core::Service;

=head1 NAME

InterMine::WebService::Core::Service - Base methods for the InterMine web
service

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::WebService::Core::Service

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2009 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use strict;
use URI;
use LWP::UserAgent;

=head2 new
 Title   : new
 Usage   : $item = InterMine::WebService::Core::Service($service_root,
                                                        "AppName");
 Function: create a new Service object

 Args    : $root_url - base url of all services, it is prefix common for all
                       services,  Example: "http://www.flymine.org/service"
           $service_relative_url - part of url specific for this service,
                            *      Example: "query/results"
           $app_name - application name, tells the server which application uses
                       the service
=cut
sub new
{
  my $class = shift;
  my $service_root = shift;
  my $app_name = shift;

  my $self = {};

  if ($service_root !~ m:/$:) {
    $service_root .= '/';
  }

  $self->{_service_root} = $service_root;
  $self->{_app_name} = $app_name;

  bless $self, $class;

  return $self;
}

=head2 get_url
 Title   : get_url
 Usage   : my $service_url = $service->get_url();
 Function: return the full URL for this service
=cut
sub get_url
{
  my $self = shift;

  return $self->{_service_root} . $self->get_relative_path();
}

=head2 get_count
 Title   : get_count
 Usage   : my $count = $service->get_count($query);
 Function: return the number of result rows for the query
=cut
sub get_count
{
  my $self = shift;
  my $query = shift;

  return $self->get_result($query, undef, undef, 1)->content();
}

=head2 
 Title   : execute_request
 Usage   : my $request =
             new InterMine::WebService::Core::Request('POST', $self->get_url(), 'TAB');
           $service->execute_request($request)
 Function: create a new Service object

 Args    : $request - a InterMine::WebService::Core::Request object
 Returns : a HTTP::Response object
=cut
sub execute_request
{
  my $self = shift;
  my $request = shift;

  my $ua = LWP::UserAgent->new();
  $ua->env_proxy();
  $ua->agent($self->{_app_name} . ' InterMine/Perl');

  if ($request->get_request_type() eq 'GET') {
    my $url = URI->new($request->get_url());
    $url->query_form($request->get_parameters());
    return $ua->get($url);
  }
  else  {
    return $ua->post($request->get_url(), {$request->get_parameters()});
  }
}

1;
