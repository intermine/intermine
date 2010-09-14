package Test::Webservice::InterMine::Constraint::Operator;

use base ('Test::Webservice::InterMine::Constraint');
use Test::More;

sub operator_methods : Test {
    my $test = shift;
    can_ok($test->class,
	   qw(operator_hash_bits op code set_code));
}

sub operator_attributes : Test(2) {
    my $test = shift;
    like(
	$test->{object}->code,
	qr/^[A-Z]$/,
	'Generates a valid code'
    );
    dies_ok(
	sub {$test->{object}->code('Q')},
	'Dies attempting to change code'
    );
}

1;
