package Webservice::InterMine::Simple;

use strict;
use Webservice::InterMine::Simple::Service;

sub get_service {
	my $self = shift;
    my ($url, $user, $pass) = @_;
    return Webservice::InterMine::Simple::Service->new(
        root => $url,
        user => $user,
        pass => $pass,
    );
}


1;

