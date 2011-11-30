=head1 NAME

Webservice::InterMine::Parser::JSON::HashRefs - 
parse rows of JSON results into hash-refs

=head1 DESCRIPTION

One of the parsers used to intepret results sent from 
the webservice.

=cut

package Webservice::InterMine::Parser::JSON::HashRefs;

use Moose;
extends 'Webservice::InterMine::Parser::JSON';
use InterMine::Model::Types qw(PathList);

=head1 ATTRIBUTES

=head2 view - The view used to set keys for the hashrefs

=cut

has view => (
    is => 'ro',
    isa => PathList,
    required => 1,
);

=head1 IMPLEMENTED PROCESSOR METHODS

The following methods implement the Processor interface.

=over 4

=item process($line) -> hash-ref

Return a hash-ref from parsing a row of data.

=back

=cut

override process => sub {
    my $self = shift;
    my $row = shift;
    my $ret = {};
    for my $col (0 .. $#{$row}) {
        my $view = $self->view->[$col] 
            or confess "There is no view for column number $col";
        my $cell = $row->[$col];
        $ret->{$view} = (ref $cell eq 'HASH') ? $cell->{value} : $cell;
    }
    unless (keys(%{$ret}) == @{$self->view}) {
        confess "There is not an output column for each view";
    }
    return $ret;
};

no Moose;
__PACKAGE__->meta->make_immutable;
1;
