package TestModel::TestField;

use Moose;
extends 'Test::Class';
with InterMine::M
use base qw(Test::Class);
use Test::More;
use Test::Exception;

my $module = 'InterMine::Query';
my @public_interface_methods = qw(
   new field_name field_type field_class
);

sub _loader : Test {
    require_ok( $module );
}

## INTERFACE TESTS
sub attributes : Test {
   can_ok($module, @public_interface_methods);
}

sub construct : Test(3) {
    new_ok($module);
    throws_ok( sub {$module::new('InterMine::Attribute
}
