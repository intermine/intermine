package Webservice::InterMine::LogicParser;

use strict;
use warnings;

use Moose;
use Webservice::InterMine::Types qw(Query);
use MooseX::Types::Moose qw(HashRef Int Str);
use Webservice::InterMine::LogicalOperator;
use Webservice::InterMine::LogicalSet;
use Scalar::Util qw(blessed);

use constant {
    LEFT_BRACKET => '(',
    RIGHT_BRACKET => ')',
    LOGICAL_OPERATOR => "Webservice::InterMine::LogicalOperator",
};

has _query => (
    isa      => Query,
    is       => 'ro',
    handles  => [qw/get_constraint/],
    weak_ref => 1,
    required => 1,
    init_arg => 'query',
);

has operators => (
    isa     => HashRef[Str],
    is      => 'ro',
    traits  => ['Hash'],
    handles => {
        valid_operator => 'exists',
        get_canonical_operator => 'get',
    },
    lazy_build => 1,
);

sub _build_operators {
    return {
        'and' => "AND",
        '&'   => "AND",
        '&&'  => "AND",
        'AND' => 'AND',

        'or'  => "OR",
        'OR'  => "OR",
        '|'   => "OR",
        '||'  => 'OR',

        LEFT_BRACKET, LEFT_BRACKET,
        RIGHT_BRACKET, RIGHT_BRACKET,
    };
}

has _operator_cache => (
    isa => 'HashRef[ ' . LOGICAL_OPERATOR . ']',
    is  => 'ro',
    traits => ['Hash'],
    handles => {
        cache_operator => 'set',
        retrieve_operator => 'get',
        is_cached => 'exists',
    },
    default => sub { {} },
);

sub get_operator {
    my $self = shift;
    my $operator = shift;
    unless ( $self->is_cached($operator) ) {
        $self->cache_operator($operator => LOGICAL_OPERATOR->new(
            token => $self->get_canonical_operator($operator),
            priority => $self->get_priority_of($operator),
        ));
    } 
    return $self->retrieve_operator($operator);
}

has priorities => (
    isa => HashRef[Int],
    is  => 'ro',
    traits => ['Hash'],
    handles => {
      get_priority_of => 'get',
    },
    lazy_build => 1,
);

sub _build_priorities {
    return {
        and => 2,
        or  => 1,
        AND => 2,
        OR  => 1,
        LEFT_BRACKET, 3,
        RIGHT_BRACKET, 3,
    };
}

sub infix_to_postfix {
    my @infix_tokens = @_;
    my (@postfix_tokens, @stack);
    while (my $token = shift @infix_tokens) {
        unless (blessed $token) {
            push @postfix_tokens, $token;
        } else {
            if ($token eq LEFT_BRACKET) {
                push @stack, $token;
            } elsif ($token eq RIGHT_BRACKET) {
                while(my $lp = pop @stack) {
                    if ($lp eq LEFT_BRACKET) {
                        if (my $stack_op = pop @stack) { 
                            push @postfix_tokens, $stack_op 
                                unless ($stack_op eq LEFT_BRACKET);
                            last;
                        } 
                    } else {
                        push @postfix_tokens, $lp;
                    }
                }
            } else {
                while (@stack && ($stack[-1]->priority <= $token->priority)) {
                    my $stack_op = pop @stack;
                    push @postfix_tokens, $stack_op 
                        unless ($stack_op eq LEFT_BRACKET);
                }
                push @stack, $token;
            }
        }
    }
    while (my $stack_op = pop @stack) {
        push @postfix_tokens, $stack_op;
    }
    return @postfix_tokens;
}

sub evaluate {
    my $self = shift;
    my @tokens = @_;
    my @stack;
    for my $token (@tokens) {
        unless (blessed $token) {
            push @stack, $token;
        } else {
            push @stack, $self->make_node($token, pop(@stack), pop(@stack));
        }
    }
    return pop(@stack);
}

sub make_node {
    my ($self, $operator, $right, $left) = @_;
    return Webservice::InterMine::LogicalSet->new(
        op    => "$operator",
        right  => (ref $right) ? $right : $self->get_constraint($right),
        left  => (ref $left) ? $left : $self->get_constraint($left),
    );
}

sub parse_logic {
    my $self = shift;
    my $input = uc(shift);
    return 
        $self->evaluate(
        infix_to_postfix(
        map   {/[()]/ ? split '' : $_}
        map   {$self->valid_operator($_) ? $self->get_operator($_) : split /\b/} 
        split /\s+/, $input));
}

sub parse_logic_without_evaluation {
    my $self = shift;
    my $input = uc(shift);
    return 
        infix_to_postfix(
        map   {/[()]/ ? split '' : $_}
        map   {$self->valid_operator($_) ? $self->get_operator($_) : split /\b/} 
        split /\s+/, $input);
}

__PACKAGE__->meta->make_immutable;
no Moose;
1;
