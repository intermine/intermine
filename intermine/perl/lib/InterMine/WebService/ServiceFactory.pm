package InterMine::WebService::ServiceFactory;

=head1 NAME

InterMine::WebService::ServiceFactory - Factory for InterMine webservice
implementations

=head1 SYNOPSIS

  my $factory = new InterMine::WebService::ServiceFactory($root, "AppName");
  my $service = $factory->get_query_service();

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::ServiceFactory

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
use warnings;

=head2 new

 Usage   : $factory = new InterMine::WebService::ServiceFactory($service_root, "AppName");
 Function: create a new ServiceFactory
 Args    : $service_root - base URL of all services, it is prefix common for
                           all services
           $app_name - application name, tells the server which application uses
                       the service
=cut
sub new
{
  my $class = shift;
  my $root_url = shift;
  my $app_name = shift;

  my $self = {};

  $self->{_root_url} = $root_url;
  $self->{_app_name} = $app_name;

  bless $self, $class;

  return $self;
}

=head2 get_query_service

 Function: return a QueryService object
 Args    : none
=cut
sub get_query_service
{
  return new InterMine::WebService::Service::QueryService($self->{_root_url},
                                                          $self->{_app_name});
}

=head2 get_template_service

 Function: return a TemplateService object
 Args    : none
=cut
sub get_template_service
{
  return new InterMine::WebService::Service::TemplateService($self->{_root_url},
                                                             $self->{_app_name});
}

=head2 get_list_service

 Function: return a ListService object
 Args    : none
=cut
sub get_list_service
{
  return new InterMine::WebService::Service::ListService($self->{_root_url},
                                                         $self->{_app_name});
}

=head2 get_model_service

 Function: return a ModelService object
 Args    : none
=cut
sub get_model_service
{
  return new InterMine::WebService::Service::ModelService($self->{_root_url},
                                                          $self->{_app_name});
}

=head2 get_service

 Function: Creates new service for general use
 Args    : $service_relative_url - part of url specific for this service
                                   eg. query/results
           $app_name - application name, tells the server which application uses
                       the service
=cut
sub get_service
{
  my $service_relative_url = shift;
  return new InterMine::Service($self->{_root_url}, $service_relative_url, $self->{_app_name});
}
