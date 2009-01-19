package InterMine::WebService::Core::Request;

=head1 NAME

InterMine::WebService::Core::Request - a web service request object
wrapping a LWP::UserAgent object, which encapsulates the server url and
request parameters

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
 Title   : new
 Usage   : $item = InterMine::WebService::Core::Request($service_root,
                                                        "AppName");
 Function: create a new QueryService object
 Args    : $request_type - should be 'GET' or 'POST'
           $service_root - base URL of the web service
           $content_type - the format for the response, 'TAB' or 'XML'
=cut
sub new {
  my $class = shift;
  my $request_type = shift;
  my $url = shift;
  my $content_type = shift // 'TAB';

  my $self = {};

  $self->{_request_type} = $request_type;
  $self->{_url} = $url;
  $self->{_content_type} = $content_type;
  $self->{_parameters} = {};

  bless $self, $class;

  return $self;
}

sub get_url
{
  my $self = shift;

  return $self->{_url};
}

sub get_request_type
{
  my $self = shift;
  return $self->{_request_type};
}

sub get_content_type
{
  my $self = shift;
  return $self->{_content_type};
}

sub get_parameters
{
  my $self = shift;
  return %{$self->{_parameters}};
}

sub add_parameters
{
  my $self = shift;
  my %new_params = @_;

  while ((my ($name, $value) = each %new_params)) {
    $self->{_parameters}->{$name} = $value;
  }
}

1;
