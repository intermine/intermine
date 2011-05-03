package Webservice::InterMine::Query::Scripted;

use Moose;
extends 'Webservice::InterMine::Query::Core';
use Webservice::InterMine::Query::Handler;

sub _build_handler {
    Webservice::InterMine::Query::Handler->new;
}

with(
    'Webservice::InterMine::Query::Roles::ReadInAble',
    'Webservice::InterMine::Query::Roles::ScriptAble',
    'Webservice::InterMine::Role::Serviced',
);

__PACKAGE__->meta->make_immutable;
no Moose;
1;
