package Webservice::InterMine::ListFactory;

use Moose;

use Webservice::InterMine::List;
use JSON -support_by_pp;
use Webservice::InterMine::Types qw(List);
use MooseX::Types::Moose qw(HashRef Str);

has string => (
    isa => Str,
    is => 'ro',
    trigger => \&process
);

has json => (
    isa => 'JSON',
    is => 'ro',
    lazy_build => 1,
    handles => ['decode'],
);

sub _build_json {
    my $self = shift;
    # Be as generous as possible to input.
    return JSON->new->utf8->relaxed->allow_singlequote->allow_barekey;
}

has lists => (
    isa => HashRef[List],
    traits => ['Hash'],
    default => sub { {} },
    handles => {
        _set_list => 'set',
        get_list_by_name => 'get',
        get_lists => 'values',
        get_list_names => 'keys',
    },
);

sub process {
    my $self = shift;
    my $str = shift;
    my $parsed = $self->decode($str);
    unless ($parsed->{wasSuccessful}) {
        confess $parsed->{error};
    }
    for my $list_info (@{$parsed->{lists}}) {
        $self->_set_list($list_info->{name}, Webservice::InterMine::List->new($list_info));
    }
}

__PACKAGE__->meta->make_immutable;
no Moose;

1;


