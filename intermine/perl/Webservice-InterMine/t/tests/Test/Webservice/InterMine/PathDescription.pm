package Test::Webservice::InterMine::PathDescription;

use base qw(Test::Webservice::InterMine::PathFeature);

use Test::More;
use Test::Exception;
use InterMine::Model;
sub class { 'Webservice::InterMine::PathDescription' }
sub args {
    my $test = shift;
    return ($test->SUPER::args, description => 'A very nice path indeed');
}
sub hash  {
    my $test = shift;
    return (
	pathString  => 'Some.path.here',
	description => 'A very nice path indeed',
    );
}
sub default_attributes {
    my $test = shift;
    return ($test->SUPER::default_attributes,
	    description => 'A very nice path indeed',);
}
sub string { 'Some.path.here: "A very nice path indeed"' }

sub build_alternatives {(
    ['Some.path', 'A less nice path'],
    [path => 'Quelqun.autre.path', description => 'a fancy-pantsy path']
)}

sub methods : Test(2) {
    my $test = shift;
    $test->SUPER::methods;
    can_ok($test->class, (qw/description/));
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

1;
