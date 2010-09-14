package Test::Webservice::InterMine::SortOrder;

use base qw(Test::Webservice::InterMine::PathFeature);

use Test::More;
use Test::Exception;

sub class { 'Webservice::InterMine::SortOrder' }
sub default_attributes {
    my $test = shift;
    return ($test->SUPER::default_attributes,
	    direction => 'asc');
}
sub string { 'Some.path.here asc' }
sub valid_directions {(
    'asc',
    'desc',
    'ASC',
    'DESC',
    'asC',
    'DeSc',
)}
sub invalid_directions {(
    'left',
    'right',
    'UP',
    'DOWN',
    'North-by-north-west'
)}
sub methods : Test(2) {
    my $test = shift;
    $test->SUPER::methods;
    can_ok($test->class, (qw/direction/));
}
sub attributes : Test(4) {
    my $test   = shift;
    $test->SUPER::attributes;
}

sub strict_construction : Test(15) {
    my $test = shift;
    $test->SUPER::strict_construction;
    for ($test->valid_directions) {
	new_ok($test->class, [$test->SUPER::args, direction => $_]);
    }
    for ($test->invalid_directions) {
	dies_ok( sub {$test->class->new($test->SUPER::args, direction => $_)},
		 "... dies trying to build with direction => $_");
    }
}

sub to_string : Test(2) {
    my $test = shift;
    $test->SUPER::to_string;
    my $obj  = $test->{object};
    is("$obj", $test->string, '... and overloads correctly');
}
1;
