package Webservice::InterMine::Constraint::Role::Operator;

use Moose::Role;
with 'Webservice::InterMine::Role::Logical';

use Carp;
use MooseX::Types::Moose qw(Str);
use Webservice::InterMine::Types qw(ConstraintCode);

has 'op' => (
    is       => 'ro',
    isa      => Str,
    required => 1,
);

has '_next_code' => (
    is => 'rw',
    isa => ConstraintCode,
    default => "A",
);

sub getNextCode {
    my $self = shift;
    my $nextcode = $self->_next_code || 'A';
    warn $nextcode if $ENV{DEBUG};
    my $thiscode = $nextcode++;
    warn $nextcode if $ENV{DEBUG};
    $self->_next_code($nextcode);
    return $thiscode;
}

has 'code' => (
    is          => 'ro',
    writer      => 'set_code',
    isa         => ConstraintCode,
    default     => sub { my $self = shift; return $self->getNextCode; },
    initializer => 'set_code',
);

sub operator_hash_bits {
    my $self = shift;
    return ( op => $self->op, code => $self->code );
}

1;
