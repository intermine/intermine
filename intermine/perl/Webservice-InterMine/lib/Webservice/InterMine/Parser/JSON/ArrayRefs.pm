package Webservice::InterMine::Parser::JSON::ArrayRefs;

=head1 NAME

Webservice::InterMine::Parser::JSON::ArrayRefs - 
parse rows of JSON results into array-refs

=head1 DESCRIPTION

One of the parsers used to intepret results sent from 
the webservice.

=cut

use Moose;
extends 'Webservice::InterMine::Parser::JSON';

=head1 IMPLEMENTED PROCESSOR METHODS

The following methods implement the Processor interface.

=over 4

=item process($line) -> array-ref

Return an array-ref from parsing a row of data.

=back

=cut

override process => sub {
    my $self = shift;
    my $row = shift;
    return [map {(ref $_ eq 'HASH') ? $_->{value} : $_} @$row];
};

no Moose;
__PACKAGE__->meta->make_immutable;
1;
