package Test::Webservice::InterMine::Constraint::Binary;

use base ('Test::Webservice::InterMine::Constraint::Unary');

use Test::More;
use Test::Exception;

sub class {'Webservice::InterMine::Constraint::Binary'};
sub hash  {(
    path => 'Some.path.here',
    op   => '=',
    value => 500,
)}
sub default_attributes {
    my $test = shift;
    return ($test->SUPER::default_attributes, value => 500);
}

sub string {'Some.path.here = "500"'}
sub args {
    my $test = shift;
    my %superargs  = $test->SUPER::args;
    @superargs{'op', 'value'} = ('=', 500);
    return (%superargs);
}

sub valid_operators {
    return (
        '=', '!=',
        '<', '>',
        '>=','<=',
        'CONTAINS',
        'LIKE',
        'NOT LIKE',
        'DOES NOT CONTAIN',
    );
}
sub invalid_operators {
    return ('IS NULL', 'LOOKUP', '%&$!');
}


sub attributes : Test(9) {
    my $test = shift;
    $test->SUPER::attributes;
    dies_ok(
	sub {$test->{object}->value('Any Value')},
	'... dies attempting to change value');
}

sub methods : Test(5) {
    my $test = shift;
    can_ok($test->class, (qw/value/));
    $test->SUPER::methods;
}

sub strict_construction : Test(27) {
    my $test = shift;
    $test->SUPER::strict_construction;
}

1;
