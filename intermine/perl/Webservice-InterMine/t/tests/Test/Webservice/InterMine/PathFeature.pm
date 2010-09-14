package Test::Webservice::InterMine::PathFeature;

use base qw(Test::Class);
use Test::More;
use Test::Exception;

sub class { 'Webservice::InterMine::PathFeature'}
sub args  { path => 'Some.path.here'}
sub string {'Some.path.here'}
sub hash   {path => 'Some.path.here'}
sub default_attributes {(
    path => 'Some.path.here',
)}

sub make_object {
    my $test = shift;
    my @args = @_;
    $test->class->new(@args);
}

sub startup : Test(startup => 1) {
    my $test = shift;
    use_ok($test->class);
}

sub object : Test(setup) {
    my $test   = shift;
    my $object = $test->make_object($test->args);
    $test->{object} = $object;
}

sub methods : Test {
    my $test = shift;
    can_ok($test->{object}, (qw/path to_string to_hash/));
}

sub attributes : Test(2) {
    my $test   = shift;
    my $object = $test->{object};
    my %attributes = $test->default_attributes;
    for (keys %attributes) {
	is_deeply(
	    $object->$_, $attributes{$_},
	    "... sets default correctly for $_",
	);
	dies_ok(
	    sub {$object->$_('Some.other.path')},
	    "... dies attempting to change $_",
	);
    }
}

sub strict_construction : Test(4) {
    my $test = shift;
    dies_ok(
	sub {$test->test_make_object()},
	'Dies constructing with too few args',
    );
    dies_ok(
	sub {$test->test_make_object($test->args, foo => 'bar')},
	"... and dies constructing with extraneous args",
    );
    lives_ok(
	sub {$test->make_object($test->args)},
	"... but lives constructing with correct args",
    );
    ok(
	$test->make_object($test->args)->isa($test->class),
	"... and the object it makes is-a " . $test->class,
    );
}

sub to_string : Test {
    my $test = shift;
    is($test->{object}->to_string, $test->string, '... Stringifies correctly');
}
sub to_hash : Test {
    my $test = shift;
    is_deeply({$test->{object}->to_hash}, {$test->hash}, '... Hashifies correctly');
}

1;
