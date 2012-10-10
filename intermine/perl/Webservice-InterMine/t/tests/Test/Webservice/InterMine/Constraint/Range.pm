package Test::Webservice::InterMine::Constraint::Range;

use base ('Test::Webservice::InterMine::Constraint::Multi');

use Test::More;
use Test::Exception;

sub class {'Webservice::InterMine::Constraint::Range'};
sub hash {(
    path => 'Some.path.here',
    op   => 'OVERLAPS',
    value => ['one', 'two', 'three'],
)}
sub default_attributes {
    my $test = shift;
    return ($test->SUPER::default_attributes, values => [qw/one two three/]);
}
sub string {'Some.path.here OVERLAPS "one, two, three"'}
sub args {
    my $test = shift;
    my %superargs  = $test->SUPER::args;
    @superargs{'op', 'values'} = ('OVERLAPS', [qw/one two three/]);
    return (%superargs);
}

sub valid_operators {
    return ('OVERLAPS', 'DOES NOT OVERLAP', 'WITHIN', 'OUTSIDE', 'CONTAINS', 'DOES NOT CONTAIN');
}
sub invalid_operators {
    return ('IS NULL', '=', 'LOOKUP', '%&$!');
}

sub strict_construction : Test(20) {
     my $test = shift;
     $test->SUPER::strict_construction;
}

1;


