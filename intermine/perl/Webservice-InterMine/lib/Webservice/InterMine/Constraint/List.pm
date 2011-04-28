package Webservice::InterMine::Constraint::List;

use Moose;

extends 'Webservice::InterMine::Constraint::Binary';

use Webservice::InterMine::Types qw(ListOperator);
use MooseX::Types::Moose qw(Str);

has '+op' => ( isa => ListOperator, coerce => 1);

__PACKAGE__->meta->make_immutable;
no Moose;
1;
