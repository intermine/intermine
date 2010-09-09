package Test::InterMine::Constraint::Multi;

use base ('Test::InterMine::Constraint::Unary');

use Test::More;
use Test::Exception;

sub class {'InterMine::Constraint::Multi'};
sub hash {(
    path => 'Some.path.here',
    op   => 'IN',
    value => ['one', 'two', 'three'],
)}
sub default_attributes {
    my $test = shift;
    return ($test->SUPER::default_attributes, values => [qw/one two three/]);
}
sub string {'Some.path.here IN "one, two, three"'}
sub args {
    my $test = shift;
    my %superargs  = $test->SUPER::args;
    @superargs{'op', 'values'} = ('IN', [qw/one two three/]);
    return (%superargs);
}

sub valid_operators {
    return ('IN', 'NOT IN',);
}
sub invalid_operators {
    return ('IS NULL', '=', 'LOOKUP', '%&$!');
}


sub attributes : Test(8) {
    my $test = shift;
    $test->SUPER::attributes;
}

sub methods : Test(5) {
    my $test = shift;
    $test->SUPER::methods;
    can_ok($test->class, (qw/values/));
}

sub strict_construction : Test(12) {
     my $test = shift;
     $test->SUPER::strict_construction;
}

1;
