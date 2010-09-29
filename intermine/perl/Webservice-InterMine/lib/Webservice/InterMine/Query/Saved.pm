package Webservice::InterMine::Query::Saved;

use Moose;
use URI;
use Webservice::InterMine::Query::SavedHandler;
use Webservice::InterMine::TemplateConstraintFactory;

sub _build_handler {
    Webservice::InterMine::Query::SavedHandler->new;
}

extends 'Webservice::InterMine::Query::Core';
with(
    'Webservice::InterMine::Query::Roles::Saved',
    'Webservice::InterMine::Query::Roles::Runnable',
    'Webservice::InterMine::Query::Roles::QueryUrl',
    'Webservice::InterMine::Query::Roles::Serviced',
    'Webservice::InterMine::Query::Roles::ReadInAble',
    'Webservice::InterMine::Query::Roles::WriteOutAble',
    'Webservice::InterMine::Query::Roles::WriteOutLegacy',
    'Webservice::InterMine::Query::Roles::ExtendedQuery',
);

__PACKAGE__->meta->make_immutable;
no Moose;
1;
