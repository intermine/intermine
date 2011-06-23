package Webservice::InterMine::LogicalOperator;

use Moose;

use overload (
    '""' => 'to_string',
    '<=>' => 'compare_num',
    fallback => 1,
);

has priority => (
    isa => 'Int',
    is => 'ro',
);

has token => (
    isa => 'Str',
    is => 'ro',
);

sub to_string {
    my $self = shift;
    return lc($self->token);
}

sub compare_num {
    my ($self, $other) = @_;
    my ($pS, $pO) = map {(blessed $_ and $_->can('priority')) ? $_->priority : 0} $self, $other;
    return $pS <=> $pO;
}

__PACKAGE__->meta->make_immutable;
no Moose;
1;
