package Webservice::InterMine::Parser::JSON::HashRefs;

use Moose;
extends 'Webservice::InterMine::Parser::JSON';
use InterMine::Model::Types qw(PathList);

has view => (
    is => 'ro',
    isa => PathList,
    required => 1,
);

override process => sub {
    my $self = shift;
    my $row = shift;
    my $ret = {};
    for my $col (0 .. $#{$row}) {
        my $view = $self->view->[$col] 
            or confess "There is no view for column number $col";
        $ret->{$view} = $row->[$col]->{value};
    }
    unless (keys(%{$ret}) == @{$self->view}) {
        confess "There is not an output column for each view";
    }
    return $ret;
};

no Moose;
__PACKAGE__->meta->make_immutable;
1;
