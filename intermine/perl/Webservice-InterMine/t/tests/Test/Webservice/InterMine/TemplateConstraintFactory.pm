package Test::Webservice::InterMine::TemplateConstraintFactory;

use base 'Test::Webservice::InterMine::ConstraintFactory';

use Test::More;
use Test::Exception;

sub class { 'Webservice::InterMine::TemplateConstraintFactory' };
sub role { 'Webservice::InterMine::Constraint::Role::Templated' };

sub test_get_constraint_class {
    my $test = shift;
    my ($type, $args, $class) = @_;
    push @$args, (editable => 1);
    $test->SUPER::test_get_constraint_class($type, $args, $class);
}


sub make_constraint : Test(18) {
    my $test = shift;
    $test->SUPER::make_constraint;
}

sub test_make_constraint {
    my $test = shift;
    my ($type, $args, $class) = @_;
    push @$args, (is_editable => 1);

    $test->SUPER::test_make_constraint($type, $args, $class);

    ok($test->class->make_constraint(@$args)->DOES($test->role),
	"... and $type instance does " . $test->role,
   );
}

1;
