package InterMine::Query::Saved;

use Moose;
use URI;
use InterMine::Query::SavedHandler;
use InterMine::TemplateConstraintFactory;

sub _build_handler {
    InterMine::Query::SavedHandler->new;
}

extends 'InterMine::Query::Core';
with (
    'InterMine::Query::Roles::Saved',
    'InterMine::Query::Roles::Runnable',
    'InterMine::Query::Roles::QueryUrl',
    'InterMine::Query::Roles::Serviced',
    'InterMine::Query::Roles::ReadInAble',
    'InterMine::Query::Roles::WriteOutAble',
    'InterMine::Query::Roles::WriteOutLegacy',
    'InterMine::Query::Roles::ExtendedQuery' => {
	type => 'saved-query',
    },
);


1;
