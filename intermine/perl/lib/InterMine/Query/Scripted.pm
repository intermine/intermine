package InterMine::Query::Scripted;

use Moose;
extends 'InterMine::Query::Core';
use InterMine::Query::Handler;

sub _build_handler {
   InterMine::Query::Handler->new;
}

with (
    'InterMine::Query::Roles::ReadInAble',
    'InterMine::Query::Roles::ScriptAble',
    'InterMine::Query::Roles::Serviced',
);


__PACKAGE__->meta->make_immutable;
no Moose;
1;
