package Test::Webservice::InterMine::Constraint::Unary;
use base ('Test::Webservice::InterMine::Constraint');
use Test::More;
use Test::Exception;

sub class {'Webservice::InterMine::Constraint::Unary'};
sub hash {(
    path => 'Some.path.here',
    op   => 'IS NULL',
)}
sub string {'Some.path.here IS NULL'}
sub args {
    my $test = shift;
    my @superargs = $test->SUPER::args;
    return (@superargs, op => 'IS NULL',);
}

sub valid_operators {
    return ('IS NOT NULL', 'IS NULL');
}
sub invalid_operators {
    return ('=', 'LOOKUP', '%&$!');
}

sub loop_args {
    my $test = shift;
    my %args = $test->args;
    delete $args{op};
    return %args;
}

sub attributes : Test(6) {
    my $test = shift;
    $test->SUPER::attributes;
    like(
	$test->{object}->code,
	qr/^[A-Z]{1,2}$/,
	'... generates a valid code'
    );
    dies_ok(
	sub {$test->{object}->code('Q')},
	'... dies attempting to change code'
    );
    ok(
	grep {$test->{object}->op eq $_} $test->valid_operators,
	"... operator is set to a valid value",
    );
    dies_ok(
	sub {$test->{object}->op('any other value')},
	"... dies attempting to change operator",
    );
}

sub inheritance : Test(4) {
    my $test = shift;
    $test->SUPER::inheritance;
    my $parent = 'Webservice::InterMine::Constraint';
    my $role1   = 'Webservice::InterMine::Constraint::Role::Operator';
    my $role2   = 'Webservice::InterMine::Role::Logical';
    ok($test->{object}->isa($parent), "... and inherits from $parent");
    ok($test->{object}->DOES($role1),  "... and does $role1");
    ok($test->{object}->DOES($role2), "... and does $role2");
}

sub strict_construction : Test(11) {
    my $test = shift;
    $test->SUPER::strict_construction;
    for ($test->valid_operators) {
	lives_ok(
	    sub {$test->make_object($test->loop_args, op => $_)},
	    "... and it lives constructing with the $_ operator",
	);
	ok(
	    $test->make_object($test->loop_args, op => $_)->isa($test->class),
	    "... and the object it makes is-a " . $test->class,
	);
    }
    for ($test->invalid_operators) {
	dies_ok(
	    sub {$test->make_object($test->loop_args, op => $_)},
	    "Dies calling with bad operator '$_'");
    }
}

sub methods : Test(4) {
    my $test = shift;
    $test->SUPER::methods;
    can_ok($test->{object}, (qw/op code set_code/));
    can_ok($test->{object},
	   qw(operator_hash_bits op code set_code));
}

sub to_hash : Test {
    my $test = shift;
    is_deeply(
	    {$test->{object}->to_hash},
	    {$test->hash, code => $test->{object}->code},
	    '... Hashifies correctly'
	);
}
1;
