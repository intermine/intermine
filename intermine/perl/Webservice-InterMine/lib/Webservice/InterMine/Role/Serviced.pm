package Webservice::InterMine::Role::Serviced;

use Moose::Role;
use Webservice::InterMine::Types qw(Service);

# Declare our generated subs so others can consume them
sub service            { }
sub service_root       { }

has service => (
    is       => 'ro',
    isa      => Service,
    coerce   => 1,
    required => 1,
    handles  => {
        service_root => 'root',
    },
);

1;
