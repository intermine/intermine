package Webservice::InterMine::Role::Described;

use Moose::Role;
use MooseX::Types::Moose qw(Str);

has description => (
    is      => 'rw',
    isa     => Str,
    default => '',
);
1;
