package InterMine::WebService::Core::Request;

=head1 NAME

InterMine::WebService::Core::Request - an object encapsulates the
server url and request parameters for an InterMine webservice client

=head1 SYNOPSIS

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::WebService::Core::Request

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

=head2 new

 Usage   : $req = InterMine::WebService::Core::Request($service_root,
                                                       "AppName");
 Function: create a new QueryService object
 Args    : $request_type - should be 'GET' or 'POST'
           $service_root - base URL of the web service
           $content_type - the format for the response, 'TAB' or 'XML'
=cut
sub new {
  my $class = shift;
  my $request_type = shift;
  my $service_root = shift;
  my $content_type = shift // 'TAB';

  my $self = {};

  $self->{_request_type} = $request_type;
  $self->{_service_root} = $service_root;
  $self->{_content_type} = $content_type;
  $self->{_parameters} = {};

  bless $self, $class;

  return $self;
}

=head2 get_url

 Function: return the service_root url for the service root that was passed to
           the constructor
=cut
sub get_url
{
  my $self = shift;

  return $self->{_service_root};
}

=head2 get_request_type

 Function: return the request type that was passed to the constructor
=cut
sub get_request_type
{
  my $self = shift;
  return $self->{_request_type};
}

=head2 get_content_type

 Function: return the content type that was passed to the constructor
=cut
sub get_content_type
{
  my $self = shift;
  return $self->{_content_type};
}

=head2 get_parameters

 Function: return the current parameters for this request
=cut
sub get_parameters
{
  my $self = shift;
  my $format;

  my %params = %{$self->{_parameters}};

  $params{format} = lc $self->get_content_type();

  return %params;
}

=head2 add_parameters

 Usage   : $req->add_parameters(query => $query, $size => 200);
 Function: add parameters to this request
=cut
sub add_parameters
{
  my $self = shift;
  my %new_params = @_;

  while ((my ($name, $value) = each %new_params)) {
    $self->{_parameters}->{$name} = $value;
  }
}

1;
