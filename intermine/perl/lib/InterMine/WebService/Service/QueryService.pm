package InterMine::WebService::Service::QueryService;

=head1 NAME

InterMine::WebService::Service::QueryService - Service for making InterMine
queries using the web service

=head1 SYNOPSIS

  my $factory = new InterMine::WebService::ServiceFactory($root, "AppName");
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

Copyright 2009 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use strict;
use warnings;

use base qw(InterMine::WebService::Core::Service);

use InterMine::WebService::Core::Request;

my $SERVICE_RELATIVE_URL = "query/results";

=head2 new

 Usage   : $sevice = InterMine::WebService::Service::QueryService($service_root,
                                                                  "AppName");
 Function: create a new QueryService object
 Args    : $service_root - base URL of the web service
           $app_name - application name, tells the server which application uses
                       the service
=cut
sub new {
  my $class = shift;
  my $root_url = shift;
  my $app_name = shift;

  my $self = {};

  if ($root_url !~ m:/$:) {
    $root_url .= '/';
  }

  $self->{_root_url} = $root_url;
  $self->{_app_name} = $app_name;

  bless $self, $class;

  return $self;
}

=head2 get_relative_path

 Usage   : my $rel_path = $service->get_relative_path();
 Function: return the path of this service relative to the base url of the
           webapp
=cut
sub get_relative_path
{
  return "query/results";
}

=head2 get_result

 Usage   : my $results = $service->get_result($query, $start, $max_count, $count_only);
 Function: return the path of this service relative to the base url of the
           webapp
 Args    : $query - the query as an XML string or as a PathQuery object
           $start - the start row to return, the first row is 1 which is the default
           $max_count - the maximum number of rows to return
           $count_only - if true, ignore $start and $max_count and instead
                         return only the number of rows as a scalar
 Returns : HTTP::Response containing the results, or a count as a scalar if
           $count_only is true
=cut
sub get_result
{
  my $self = shift;
  my $query = shift;
  my $start = shift;
  my $max_count = shift;
  my $count_only = shift;

  if (ref $query) {
    $query = $query->to_string();
  }

  $count_only //= 0;

  my $request =
    new InterMine::WebService::Core::Request('POST', $self->get_url(), 'TAB');

  if ($count_only) {
    $request->add_parameters(tcount => '');
  } else {
    $start //= 1;
    $max_count //= 100;
    $request->add_parameters(start => $start);
    $request->add_parameters(size => $max_count);
  }
  $request->add_parameters(query => $query);
  return $self->execute_request($request);
}

1;
