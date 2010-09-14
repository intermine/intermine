package Test::Webservice::InterMine::Constraint::Ternary;

use base ('Test::Webservice::InterMine::Constraint::Binary');

use Test::More;
use Test::Exception;
use InterMine::Model; # Again, no idea why, but this test class does not compile
                      # without loading this module first?!?
sub class {'Webservice::InterMine::Constraint::Ternary'};
sub hash {(
    path => 'Some.path.here',
    op   => 'LOOKUP',
    value => 500,
    extraValue => 'Drosophila',
)}
sub default_attributes {
    my $test = shift;
    return ($test->SUPER::default_attributes, extra_value => 'Drosophila');
}
sub string {'Some.path.here LOOKUP "500" IN "Drosophila"'}
sub args {
    my $test = shift;
    my %superargs  = $test->SUPER::args;
    @superargs{'op', 'extra_value'} = ('LOOKUP', 'Drosophila');
    return (%superargs);
}

sub valid_operators {
    return (
        'LOOKUP',
    );
}
sub invalid_operators {
    return ('IS NULL', '=', 'IS ONE OF', '%&$!');
}

sub inheritance : Test(5) {
    my $test   = shift;
    $test->SUPER::inheritance;
    my $parent = 'Webservice::InterMine::Constraint::Binary';
    ok($test->class->isa($parent), "... and from $parent");
}

sub attributes : Test(11) {
    my $test = shift;
    $test->SUPER::attributes;
}

sub methods : Test(6) {
    my $test = shift;
    $test->SUPER::methods;
    can_ok($test->class, (qw/extra_value/));
}

sub strict_construction : Test(10) {
    my $test = shift;
    $test->SUPER::strict_construction;
}

1;
