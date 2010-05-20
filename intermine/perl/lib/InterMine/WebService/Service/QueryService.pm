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

use IO::String;
use Text::CSV_XS;

use InterMine::WebService::Core::Request;

my $SERVICE_RELATIVE_URL = "query/results";
my $query_error = "No query passed\n";

=head2 new

 Usage   : $sevice = InterMine::WebService::Service::QueryService($service_root,
                                                                  "AppName");
 Function: create a new QueryService object
 Args    : $service_root - base URL of the web service
           $app_name - application name, tells the server which application uses
                       the service

=cut
sub new
{
  my $class = shift;
  my $service_root = shift;
  my $app_name = shift;

  my $self = $class->SUPER::new($service_root, $app_name);
  $self->{_SERVICE_RELATIVE_URL} = $SERVICE_RELATIVE_URL;
  bless $self, $class;

  return $self;
}

=head2 get_result

 Usage   : my $results = $service->get_result($query, $start, $max_count, $count_only);
 Function: get the results of a query
 Args    : $query - the query as an XML string or as a PathQuery object
           $start - the start row to return, the first row is 0 which is the default
           $max_count - the maximum number of rows to return (undef means get 100)
 Returns : HTTP::Response containing the results.

=cut
sub get_result
{
  my $self = shift;
  my $query = shift;
  die $query_error unless $query;
  my $start = shift;
  my $max_count = shift;
  my $count_only = shift; # Support for this feature has been discontinued

  if (ref $query) {
    $query = $query->to_xml_string();
  }

  if (!defined $count_only) {
    $count_only = 0;
  }

  my $request =
    new InterMine::WebService::Core::Request('GET', $self->get_url(), 'TAB');

  if ($count_only) { # Support for this feature has been discontinued
    $request->add_parameters(tcount => '');
  } else {
    if (!defined $start) {
# Result count starts at 0
      $start = 0;
    }
    if (!defined $max_count) {
      $max_count = 100;
    }
    $request->add_parameters(start => $start);
    $request->add_parameters(size => $max_count);
  }
  $request->add_parameters(query => $query);
  return $self->execute_request($request);
}

=head2 get_result_table

 Usage   : my $result_table = $service->get_result_table($query, $start, $max_count);
 Function: return the path of this service relative to the base url of the
           webapp
 Args    : $query - the query as an XML string or as a PathQuery object
           $start - the start row to return, the first row is 1 which is the default
           $max_count - the maximum number of rows to return
 Returns : a table of results as a list of list, eg. (["Gene1", 100], [Gene2, 200])

=cut

sub get_result_table
{
  my $self = shift;
  my $query = shift;
  my $start = shift;
  my $max_count = shift;

  my $response = $self->get_result($query, $start, $max_count);

  if ($response->is_success()) {
    my $io = new IO::String($response->content());

    my @retval = ();

    my $csv = new Text::CSV_XS({sep_char => "\t"});

    while (!$io->eof()) {
      push @retval, $csv->getline ($io);
    }

    return @retval;
  } else {
    die "request failed with error: ", $response->status_line(), "\n", $response->content(), "\n";
  }
}

=head2 get_count (NOT IMPLEMENTED)

 Title   : get_count
 Usage   : my $count = $service->get_count($query);
 Function: return the number of result rows for the query
 
 WARNING Support for this feature this feature has been discontinued
 on the server side

=cut
sub get_count
{
# WARNING suport for this feature has been discontinued
  my $self = shift;
  my $query = shift;

  return $self->get_result($query, undef, undef, 1)->content();
}


1;
