package Webservice::InterMine::Role::Named;

use Moose::Role;
use MooseX::Types::Moose qw(Str);

has name => (
    is      => 'rw',
    isa     => Str,
    default => '',
);
1;
