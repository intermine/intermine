package Webservice::InterMine::Query::Roles::Serviced;

use Moose::Role;
use InterMine::TypeLibrary qw(Service);

# Declare our generated subs so other can consume them
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
