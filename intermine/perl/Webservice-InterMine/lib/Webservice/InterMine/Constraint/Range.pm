package Webservice::InterMine::Constraint::Range;

use Moose;

extends 'Webservice::InterMine::Constraint::Multi';

use Webservice::InterMine::Types qw(RangeOperator);

has '+op' => ( isa => RangeOperator, coerce => 1 );

__PACKAGE__->meta->make_immutable;

no Moose;

1;
