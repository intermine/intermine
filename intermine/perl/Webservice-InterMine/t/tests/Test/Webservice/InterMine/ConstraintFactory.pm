package Test::Webservice::InterMine::ConstraintFactory;

use base 'Test::Class';

use Test::More;
use Test::Exception;

sub class { 'Webservice::InterMine::ConstraintFactory' };
sub types {
    my $test  = shift;
    my @types = sort keys %{$test->constraint_types};
    return @types;
}
sub constraint_types {{
    unary => {
	args => [path => 'Some.path', op => 'IS NULL'],
	class => 'Webservice::InterMine::Constraint::Unary',
    },
    binary => {
	args => [path => 'Some.path', op => '=', value => 500],
	class => 'Webservice::InterMine::Constraint::Binary',
    },
    ternary => {
	args => [path => 'Some.path', op => 'LOOKUP',
		 value => 500, extra_value => 'NZ'],
	class => 'Webservice::InterMine::Constraint::Ternary',
    },
    multi => {
	args => [path => 'Some.path', op => 'ONE OF', values => [qw/a b c/]],
	class => 'Webservice::InterMine::Constraint::Multi',
    },
    subclass => {
	args => [path => 'Some.path', type => 'Some.other.path'],
	class => 'Webservice::InterMine::Constraint::SubClass',
    },
    range => {
        args => [path => "Some.path", op => 'OVERLAPS', values => [qw/foo bar baz/]],
        class => 'Webservice::InterMine::Constraint::Range',
    }
}}

sub startup : Test(startup => 1) {
    my $test = shift;
    use_ok($test->class);
}

sub get_constraint_class : Test(9) {
    my $test = shift;
   throws_ok(
       sub {$test->class->get_constraint_class()},
	qr/No suitable constraint class found/,
	"... catches no arguments",
    );

    throws_ok(
	sub {$test->class->get_constraint_class('foo', 'bar')},
	qr/No suitable constraint class found/,
	"... catches bad arguments - even",
    );

    throws_ok(
	sub {$test->class->get_constraint_class('foo', 'bar', 'baz')},
	qr/Odd number of elements/,
	"... catches bad arguments - odd",
    );

    for my $type ($test->types) {
	my $args  = $test->constraint_types->{$type}->{args};
	my $class = $test->constraint_types->{$type}->{class};
	$test->test_get_constraint_class($type, $args, $class);
    }
}

sub test_get_constraint_class {
    my $test = shift;
    my ($type, $args, $class) = @_;
    lives_ok(
	sub {$test->class->get_constraint_class(@$args)},
	"... handles $type arguments ok",
    );
}

sub make_constraint : Test(12) {
    my $test = shift;
    for my $type ($test->types) {
	my $args  = $test->constraint_types->{$type}->{args};
	my $class = $test->constraint_types->{$type}->{class};
	$test->test_make_constraint($type, $args, $class);
    }
}

sub test_make_constraint {
    my $test = shift;
    my ($type, $args, $class) = @_;

    lives_ok(
	sub {$test->class->make_constraint(@$args);},
	"... makes a $type constraint",
    );

    ok($test->class->make_constraint(@$args)->isa($class),
       "... and it is a proper instance of its class",
   );
}
1;
