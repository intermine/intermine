package TestQuery;

use base qw(Test::Class);
use Test::More;

my $module = 'InterMine::Query';
my @public_interface_methods = qw(
   name description view views add_view sort_order set_sort_order
   constraints add_constraint find_constraint coded_constraint
   editable_constraints set_type get_type joins add_join
   path_descriptions add_pathdescription logic all_paths
   model get_constraint_class make_constraint
);
sub _loader : Test {
    require_ok( $module );
}

## INTERFACE TESTS
sub attributes : Test {
   can_ok($module, @public_interface_methods);
}
