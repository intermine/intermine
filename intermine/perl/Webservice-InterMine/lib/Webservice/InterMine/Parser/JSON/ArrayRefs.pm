package Webservice::InterMine::Parser::JSON::ArrayRefs;

use Moose;
extends 'Webservice::InterMine::Parser::JSON';

override process => sub {
    my $self = shift;
    my $row = shift;
    return [map {$_->{value}} @$row];
};

no Moose;
__PACKAGE__->meta->make_immutable;
1;
