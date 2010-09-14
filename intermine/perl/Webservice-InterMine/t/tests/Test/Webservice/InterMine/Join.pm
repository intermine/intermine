package Test::Webservice::InterMine::Join;

use base qw(Test::Webservice::InterMine::PathFeature);

use Test::More;
use Test::Exception;
use InterMine::Model;
sub class { 'Webservice::InterMine::Join' }
sub string { 'Some.path.here is an OUTER join' }
sub hash  {
    my $test = shift;
    return ($test->args, style => 'OUTER');
}
sub default_attributes {
    my $test = shift;
    return ($test->SUPER::default_attributes, style => 'OUTER');
}
sub build_alternatives {(
    ['Some.path'],
    ['Some.path', 'OUTER'],
)}
sub valid_styles {
    return qw(
	INNER
	OUTER
    );
}
sub invalid_styles {
    return qw(
	PLAID
	BOHEMIAN
	outer
    );
}
sub methods : Test(2) {
    my $test = shift;
    $test->SUPER::methods;
    can_ok($test->class, (qw/style/));
}
sub attributes : Test(4) {
    my $test   = shift;
    $test->SUPER::attributes;
}

sub alternative_build :Test(2) {
    my $test = shift;
    for ($test->build_alternatives) {
	new_ok($test->class, $_);
    }
}

sub strict_construction : Test(9) {
    my $test = shift;
    $test->SUPER::strict_construction;
    for ($test->valid_styles) {
	new_ok($test->class, [$test->SUPER::args, style => $_]);
    }
    for ($test->invalid_styles) {
	dies_ok( sub {$test->class->new($test->SUPER::args, style => $_)},
		 "... dies trying to build with style => $_");
    }
}
1;
