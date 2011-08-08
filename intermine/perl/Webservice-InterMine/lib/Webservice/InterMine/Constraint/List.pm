package Webservice::InterMine::Constraint::List;

use Moose;

extends 'Webservice::InterMine::Constraint::Binary';

=head1 NAME

Webservice::InterMine::Constraint::List

=head1 SYNOPSIS

    $query->where(Gene => {in => "Some List"});
    $query->where(Gene => {not_in => "Some List"});
    $query->where(Gene => $service->get_list("Some List"));
    $query->where("Gene", "IN", "Some List");
    $query->where("Gene", "NOT IN", "Some List");

=head2 DESCRIPTION

Constraints that require an object to be contained in a pre-existing
list in the webservice.

Valid operators are "IN" and "NOT IN".

=cut

use Webservice::InterMine::Types qw(ListOperator ListName);
use MooseX::Types::Moose qw(Str);

has '+op' => ( isa => ListOperator, coerce => 1);
has '+value' => ( isa => ListName, coerce => 1);

__PACKAGE__->meta->make_immutable;
no Moose;
1;
