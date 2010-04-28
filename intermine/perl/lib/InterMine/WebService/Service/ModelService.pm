package InterMine::WebService::Service::ModelService;

=head1 NAME

InterMine::WebService::Service::ModelService - Service for making InterMine
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

    perldoc InterMine::WebService::Service::ModelService

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
use InterMine::Model;

my $SERVICE_RELATIVE_URL = "model";

=head2 new

 Usage   : $sevice = InterMine::WebService::Service::ModelService($service_root,
                                                                  "AppName");
 Function: create a new ModelService object
 Args    : $service_root - base URL of the web service
           $app_name - application name, tells the server which application uses
                       the service
=cut
sub new
{
  my $class = shift;
  my $root_url = shift;
  my $app_name = shift;

  my $self = $class->SUPER::new($root_url, $app_name);

  bless $self, $class;

  return $self;
}


=head2 get_relative_path

 Usage   : my $rel_path = $service->get_relative_path();
 Function: return the path of this service relative to the base url of the
           webapp

=cut

sub get_relative_path {
    return $SERVICE_RELATIVE_URL;
}


=head2 get_model_xml

 Usage   : $model_resp = $service->get_model_xml();
           if ($model_resp->is_success) {
             $model = new InterMine::Model(string => $model_resp->content()));
           }
 Function: get the model XML for this webapp
 Returns : HTTP::Response containing the model
=cut

sub get_model_xml
{
  my $self = shift;
  my $request =
    new InterMine::WebService::Core::Request('GET', $self->get_url(), 'TEXT');

  return $self->execute_request($request);
}

=head2 get_model

 Usage   : $model = $service->get_model();
 Function: get the model XML for this webapp
 Returns : HTTP::Response containing the model
 
=cut
sub get_model
{
  my $self = shift;

  my $resp = $self->get_model_xml();

  if ($resp->is_error()) {
    die 'fetching model failed with message: ', $resp->status_line(), "\n";
  } else {
    return new InterMine::Model(string => $resp->content());
  }
}

1;
