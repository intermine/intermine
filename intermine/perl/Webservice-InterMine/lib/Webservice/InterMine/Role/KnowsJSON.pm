package Webservice::InterMine::Role::KnowsJSON;

use Moose::Role;
use JSON -support_by_pp;

has json => (
    isa => 'JSON',
    is => 'ro',
    lazy_build => 1,
    handles => ['decode'],
);

sub _build_json {
    my $self = shift;
    # Be as generous as possible to input.
    return JSON->new->relaxed->allow_singlequote->allow_barekey->allow_nonref;
}

1;

