package Test::Webservice::InterMine::Constraint::SubClass;

use base ('Test::Webservice::InterMine::Constraint');

use Test::More;
use Test::Exception;

sub class {'Webservice::InterMine::Constraint::SubClass'};
sub hash {(
    path => 'Some.path.here',
    type => 'Manager',
)}
sub default_attributes {
    my $test = shift;
    return ($test->SUPER::default_attributes, type => 'Manager');
}
sub string {'Some.path.here is a Manager'}
sub args {
    my $test = shift;
    my @superargs  = $test->SUPER::args;
    return (@superargs, type => 'Manager');
}

sub inheritance : Test(2) {
    my $test = shift;
    $test->SUPER::inheritance;
    my $parent = 'Webservice::InterMine::Constraint';
    ok($test->class->isa($parent), "... and from $parent");
}

sub attributes : Test(4) {
    my $test = shift;
    $test->SUPER::attributes;
}

sub methods : Test(3) {
    my $test = shift;
    can_ok($test->class, (qw/type/));
    $test->SUPER::methods;
}


1;
