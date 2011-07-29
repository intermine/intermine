package Webservice::InterMine::Parser::JSON::ResultRows;

=head1 NAME

Webservice::InterMine::Parser::JSON::ResultRows - 
parse rows of JSON results into L<ResultRow>s

=head1 DESCRIPTION

One of the parsers used to intepret results sent from 
the webservice.

=cut

use Moose;
extends 'Webservice::InterMine::Parser::JSON';

use Webservice::InterMine::ResultRow;
use InterMine::Model::Types qw/PathList/;

has view => (
    is => 'ro', 
    isa => PathList,
    required => 1,
);

override process => sub {
    my $self = shift;
    my $row = shift;
    return Webservice::InterMine::ResultRow->new(views => $self->view, cells => $row);
};

no Moose;
__PACKAGE__->meta->make_immutable;
1;

