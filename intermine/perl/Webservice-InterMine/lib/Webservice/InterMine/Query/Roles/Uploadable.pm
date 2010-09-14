package Webservice::InterMine::Query::Roles::Uploadable;

use Moose::Role;

requires qw/service to_xml upload_url/;

sub upload {
    my $self    = shift;
    my $xml     = $self->to_xml;
    my $url     = $self->upload_url;
    my $service = $self->service;
    my $result  = $service->send_off( $xml => $url );
    return $result;
}

1;
