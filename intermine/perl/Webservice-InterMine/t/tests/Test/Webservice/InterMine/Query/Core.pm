package Test::Webservice::InterMine::Query::Core;

use strict;
use warnings;
use base qw(Test::Class);
use List::MoreUtils qw(uniq);
use Test::More;
use Test::MockObject;
use Test::Exception;
use Set::CrossProduct;
use InterMine::Model::TestModel;
use Webservice::InterMine::ConstraintFactory;
sub class {'Webservice::InterMine::Query::Core'}
sub args {my $test = shift; return (model => $test->model);}
sub service_url {'test.url.string'}
sub model {return InterMine::Model::TestModel->instance};
sub logic_string1 {'A and B and C'}
sub logic_string2 {'(A or B) and C'}
sub extra_constraint_args {}
sub test_paths {
    my @paths = (
	'Employee.name', 'Employee.department.name',
	'Employee', 'Employee.age'
    );
    return @paths;
}
sub startup : Test(startup => 3) {
    my $test = shift;
    use_ok($test->class);
    my $q;
    lives_ok {$q = $test->class->new($test->args)} "Can make a query";
    isa_ok($q, $test->class);
}

sub setup : Test(setup) {
    my $test      = shift;
    my $object    = $test->class->new($test->args);
    $test->{object} = $object;
}

sub teardown : Test(teardown) {
    my $test = shift;
    undef $test->{object};
}

sub _methods : Test {
    my $test    = shift;
    # Public API methods
    my @methods = (
	qw/
	      name description sort_order set_sort_order view add_view
	      constraints add_constraint find_constraints
	      map_constraints coded_constraints parse_constraint_string
	      type_dict subclasses joins all_joins map_joins add_join
	      path_descriptions all_path_descriptions
	      map_path_descriptions add_pathdescription logic all_paths
	      validate validate_paths validate_sort_order
	      validate_subclass_constraints select where
	  /
      );
    can_ok($test->class, @methods);
}


sub _attributes : Test(7) {
    my $test   = shift;
    my @readonly_attrs = (qw/
	sort_order view constraints joins
	path_descriptions model constraint_factory
    /);
    for (@readonly_attrs) {
        dies_ok(
            sub {$test->{object}->$_('Some.other.value')},
            "... dies attempting to change $_",
        );
    }
}

sub add_unary_constraint : Test(12) {
    my $test = shift;
    my $obj  = $test->{object};
    my $con;
    lives_ok {$con = $obj->add_constraint(path => 'Employee.fullTime', op => 'IS NOT NULL')} "makes a unary constraint without dying";
    isa_ok($con, 'Webservice::InterMine::Constraint::Unary', ".. and it");

    lives_ok {$con = $obj->add_constraint(fullTime => 'IS NULL')} "can use short syntax";
    isa_ok($con, 'Webservice::InterMine::Constraint::Unary', ".. and it");

    lives_ok {$con = $obj->add_constraint(age => undef)} "can use DBIx-ish syntax";
    isa_ok($con, 'Webservice::InterMine::Constraint::Unary', ".. and it");

    lives_ok {$con = $obj->add_constraint(age => {'!=' => undef})} "can use DBIx-ish syntax";
    isa_ok($con, 'Webservice::InterMine::Constraint::Unary', ".. and it");

    lives_ok {$con = $obj->add_constraint('Employee.department.company.vatNumber IS NULL')} 
        "makes a unary from parsing without dying";
    isa_ok($con, 'Webservice::InterMine::Constraint::Unary', ".. and it");

    lives_ok {$con = $obj->add_constraint('Employee.department.company.vatNumber IS NOT NULL')} 
        "makes a unary from parsing without dying";
    isa_ok($con, 'Webservice::InterMine::Constraint::Unary', ".. and it");
}

sub add_binary_constraint:Test(24) {
    my $test = shift;
    my $obj  = $test->{object};
    my $con;
    lives_ok(
        sub {
            $con = $obj->add_constraint(
                path => 'Employee.age',
                op   => '<',
                value => 16,
                $test->extra_constraint_args,
            );
        },
        "makes a binary constraint without dying",
    );
    isa_ok($con, 'Webservice::InterMine::Constraint::Binary', ".. and it");

    lives_ok {$con = $obj->add_constraint(qw/age > 10/)} "Can use short form";
    isa_ok($con, 'Webservice::InterMine::Constraint::Binary', ".. and it");

    lives_ok {$con = $obj->add_constraint(age => 10)} "Can use DBIx-ish form";
    isa_ok($con, 'Webservice::InterMine::Constraint::Binary', ".. and it");
    is($con->path, "Employee.age");
    is($con->op, '=');
    is($con->value, 10);

    lives_ok {$con = $obj->add_constraint(age => {'>' =>  10})} "Can specify operator in DBIx-ish syntax";
    isa_ok($con, 'Webservice::InterMine::Constraint::Binary', ".. and it");
    is($con->path, "Employee.age");
    is($con->op, '>');
    is($con->value, 10);

    lives_ok {$con = $obj->add_constraint(age => {ge => 12})} "Can specify operator in DBIx-ish syntax with word ops";
    isa_ok($con, 'Webservice::InterMine::Constraint::Binary', ".. and it");
    is($con->path, "Employee.age");
    is($con->op, '>=');
    is($con->value, 12);

    lives_ok {$con = $obj->add_constraint('Employee.department.company.address.address != A lovely place')} 
        "makes a binary from parsing without dying";
    isa_ok($con, 'Webservice::InterMine::Constraint::Binary', ".. and it");
    is($con->path, "Employee.department.company.address.address");
    is($con->op, '!=');
    is($con->value, 'A lovely place');

}

sub remove_constraint:Test(4) {
    my $test = shift;
    my $obj = $test->{object};
    my $initial_count = $obj->count_constraints;
    my $conA = $obj->add_constraint("Employee.age", "=", "Foo");
    my $conB = $obj->add_constraint("Employee.age", "=", "Boo");
    is($initial_count + 2, $obj->count_constraints, "Initial constraint count is correct");
    $obj->remove_constraint($conA);
    is($initial_count + 1, $obj->count_constraints, "Can remove with obj");
    $obj->remove_constraint($conB->code);
    is($initial_count + 0, $obj->count_constraints, "Can remove with code");
    dies_ok {$obj->remove_constraint("ZZ")} "Dies on non existent constraints";
}

sub add_loop_constraint:Test(6) {

    my $test = shift;
    my $obj  = $test->{object};
    my $con;

    $obj->add_views("Employee.department.manager", "Employee.department.company.CEO");

    lives_ok(
        sub {
            $con = $obj->add_constraint(
                path => 'Employee.department.manager',
                op   => 'IS',
                loop_path => 'Employee.department.company.CEO',
                $test->extra_constraint_args,
            );
        },
        "Can make a loop constraint"
    );
    isa_ok($con, 'Webservice::InterMine::Constraint::Loop', ".. and it");

    lives_ok {$con = $obj->add_constraint('department.manager' => $obj->path('department.company.CEO'))}
        "can make a loop constraint in more succinct notation";

    isa_ok($con, 'Webservice::InterMine::Constraint::Loop', ".. and it");
    
    lives_ok {$con = $obj->add_constraint('department.manager' => {isnt => $obj->path('department.company.CEO')})}
        "can make a negative loop constraint in more succinct notation";

    isa_ok($con, 'Webservice::InterMine::Constraint::Loop', ".. and it");

    # dies_ok {$obj->path('department.company.vatNumber')} "Throws errors at paths not in the query";
}

sub add_ternary_constraint:Test(15) {
    my $test = shift;
    my $obj  = $test->{object};
    my $con;

    lives_ok(
        sub {
            $con = $obj->add_constraint(
                path => 'Employee.department',
                op   => 'LOOKUP',
                value => 'Post Room',
                extra_value => 'Woolworths',
                $test->extra_constraint_args,
            );
        },
        "makes a ternary constraint with extra_value without dying",
    );
    isa_ok($con, 'Webservice::InterMine::Constraint::Ternary', ".. and it");

    lives_ok(
        sub {
            $con =  $obj->add_constraint(
                path => 'Employee.address',
                op   => 'LOOKUP',
                value => '14 Mill Lane',
                $test->extra_constraint_args,
            );
        },
        "makes a ternary constraint without extra_value without dying",
    );
    isa_ok($con, 'Webservice::InterMine::Constraint::Ternary', ".. and it");

    lives_ok {$con = $obj->add_constraint(department => {lookup => 'Sales'})} "Can make a lookup with DBIx sugar";
    isa_ok($con, 'Webservice::InterMine::Constraint::Ternary', ".. and it");
    is("Employee.department", $con->path);
    is("LOOKUP", $con->op);
    is("Sales", $con->value);

    lives_ok {$con = $obj->add_constraint(department => {lookup => 'Foo', extra_value => 'WH'})} "Can make a lookup with DBIx sugar";
    isa_ok($con, 'Webservice::InterMine::Constraint::Ternary', ".. and it");
    is("Employee.department", $con->path);
    is("LOOKUP", $con->op);
    is("Foo", $con->value);
    is("WH", $con->extra_value);
}

sub add_list_constraint:Tests {
    my $test = shift;
    my $obj = $test->{object};

    # Mock objects
    my $list_obj = Test::MockObject->new();
    $list_obj->set_isa("Webservice::InterMine::List");
    $list_obj->mock(name => sub {"Some List"});
    my $sub_query = Test::MockObject->new();
    $sub_query->set_isa("Webservice::InterMine::Query");
    $sub_query->mock(to_list_name => sub {"Some List"});
    my %expected_ops = (in => "IN", not_in => "NOT IN", "" => "IN");
    my $iterator = Set::CrossProduct->new([
            ["Some List", $list_obj, $sub_query],
            ["IN", "NOT IN", "in", "not_in", ""],
            ["list", "pair"],
        ]);
    while (my $combination = $iterator->get) {
        my @args;
        if ($combination->[2] eq "list") {
            @args = ("Employee", $combination->[1] || "IN", $combination->[0]);
        } else {
            if ($combination->[1]) {
                @args = ("Employee" => {$combination->[1] => $combination->[0]});
            } else {
                next unless (ref $combination->[0]);
                @args = ("Employee" => $combination->[0]);
            }
        }
        my $op = $expected_ops{$combination->[1]} || $combination->[1];

        my $con;

        lives_ok(
            sub {$con = $obj->add_constraint(@args)},
            "Can add a list constraint",
        );
        isa_ok($con, "Webservice::InterMine::Constraint::List", "And it")
            or (diag explain $combination);
        is($op, $con->op, "It has the right operator");
        is("Some List", $con->value, "It has the right value");
    }
}

sub add_subclass_constraint:Test(5) {
    my $test = shift;
    my $obj  = $test->{object};
    my $con;

    lives_ok(
        sub {
            $con = $obj->add_constraint(
                path => 'Employee',
                type => 'Manager',
                $test->extra_constraint_args,
            );
        },
        "makes a subclass constraint without dying",
    );
    isa_ok($con, 'Webservice::InterMine::Constraint::SubClass', ".. and it");
}

sub add_multi_value_constraint:Test(10) {
    my $test = shift;
    my $obj  = $test->{object};
    my $con;
    lives_ok(
        sub {
            $con = $obj->add_constraint(
                path => 'Employee.name',
                op   => 'ONE OF',
                values => [qw/Lacky Lickspittle Dogsbody/],
                $test->extra_constraint_args,
            );
        },
        "makes a multi constraint"
    );
    isa_ok($con, 'Webservice::InterMine::Constraint::Multi', ".. and it");

    lives_ok {$con = $obj->add_constraint('name' => [qw/Bob Bill Brenda/])} "Can use DBIx-ish sugar";
    isa_ok($con, 'Webservice::InterMine::Constraint::Multi', ".. and it");

    lives_ok {$con = $obj->add_constraint('age' => [20 .. 30])} "Can use DBIx-ish sugar with ranges";
    isa_ok($con, 'Webservice::InterMine::Constraint::Multi', ".. and it");

    lives_ok {$con = $obj->add_constraint('age' => {'none of' => [30 .. 40]})} "Can use DBIx-ish sugar with ranges";
    isa_ok($con, 'Webservice::InterMine::Constraint::Multi', ".. and it");
    is($con->op, 'NONE OF');
    is_deeply($con->values, [30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40]);

}

sub add_subclass_problem:Test(5) {
    my $test = shift;
    my $obj  = $test->{object};

    dies_ok(
        sub {$obj->add_constraint(path => 'Manager', type => 'Employee')},
        "Dies adding an invalid subclass constraint - not a subclass"
    );
}

sub add_bad_subclass:Test(1) {
    my $test = shift;
    my $obj  = $test->{object};

    dies_ok(
        sub {$obj->add_constraint(path => 'Employee', type => 'Quux')},
        "Dies adding an invalid subclass constraint - bad class"
    );
}

sub add_inconsistent_constraints:Test(1) {
    my $test = shift;
    my $obj  = $test->{object};

    $obj->add_constraint('Employee.name = "Foo"');
    dies_ok(
        sub {$obj->add_constraint('Company.name = "Bar"')},
        "Dies adding an inconsistent constraint",
    );
}

sub add_invalid_path_on_constraint:Test(1) {
    my $test = shift;
    my $obj  = $test->{object};
    dies_ok(sub {$obj->add_constraint('Foo.bar IS NULL')}, "Dies adding a constraint with an invalid path");
}

sub add_dbix_style_search:Test(19) {
    my $test = shift;
    my $obj  = $test->{object};
    $obj->_set_root('Employee');
    $obj->clear_constraints;

    lives_ok {
        $obj->search({
            name => 'M*',
            age => [25 .. 35],
            department => {lookup => 'Warehouse'},
            end => undef,
            Employee => {in => 'some-list'}
        });
    } "Can use dbix search syntax";

    my @constraints = $obj->constraints;

    # List and Ternary inherit from binary
    is (grep({$_->isa('Webservice::InterMine::Constraint::Binary')} @constraints), 3)
        or diag(explain [grep {$_->isa('Webservice::InterMine::Constraint::Binary')} @constraints]);
    my ($binary) = grep({$_->path eq 'Employee.name'} @constraints);
    is('M*', $binary->value);
    is('=', $binary->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::Unary')} @constraints), 1);
    my ($unary) = grep({$_->isa('Webservice::InterMine::Constraint::Unary')} @constraints);
    is("Employee.end", $unary->path);
    is('IS NULL', $unary->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::Multi')} @constraints), 1);
    my ($multi) = grep({$_->isa('Webservice::InterMine::Constraint::Multi')} @constraints);
    is("Employee.age", $multi->path);
    is_deeply([25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35], $multi->values);
    is('ONE OF', $multi->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::Ternary')} @constraints), 1);
    my ($ternary) = grep({$_->isa('Webservice::InterMine::Constraint::Ternary')} @constraints);
    is("Employee.department", $ternary->path);
    is('Warehouse', $ternary->value);
    is('LOOKUP', $ternary->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::List')} @constraints), 1);
    my ($list) = grep({$_->isa('Webservice::InterMine::Constraint::List')} @constraints);
    is("Employee", $list->path);
    is('some-list', $list->value);
    is('IN', $list->op);
}

sub add_dbix_style_search_with_order:Test(19) {
    my $test = shift;
    my $obj  = $test->{object};
    $obj->_set_root('Employee');
    $obj->clear_constraints;

    lives_ok {
        $obj->search([
            name => 'M*',
            age => [25 .. 35],
            department => {lookup => 'Warehouse'},
            end => undef,
            Employee => {in => 'some-list'}
        ]);
    } "Can use dbix search syntax";

    my @constraints = $obj->constraints;

    # List and Ternary inherit from binary
    is (grep({$_->isa('Webservice::InterMine::Constraint::Binary')} @constraints), 3)
        or diag(explain [grep {$_->isa('Webservice::InterMine::Constraint::Binary')} @constraints]);
    my ($binary) = grep({$_->path eq 'Employee.name'} @constraints);
    is('M*', $binary->value);
    is('=', $binary->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::Unary')} @constraints), 1);
    my ($unary) = grep({$_->isa('Webservice::InterMine::Constraint::Unary')} @constraints);
    is("Employee.end", $unary->path);
    is('IS NULL', $unary->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::Multi')} @constraints), 1);
    my ($multi) = grep({$_->isa('Webservice::InterMine::Constraint::Multi')} @constraints);
    is("Employee.age", $multi->path);
    is_deeply([25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35], $multi->values);
    is('ONE OF', $multi->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::Ternary')} @constraints), 1);
    my ($ternary) = grep({$_->isa('Webservice::InterMine::Constraint::Ternary')} @constraints);
    is("Employee.department", $ternary->path);
    is('Warehouse', $ternary->value);
    is('LOOKUP', $ternary->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::List')} @constraints), 1);
    my ($list) = grep({$_->isa('Webservice::InterMine::Constraint::List')} @constraints);
    is("Employee", $list->path);
    is('some-list', $list->value);
    is('IN', $list->op);
}

sub where_search:Test(21) {
    my $test = shift;
    my $obj  = $test->{object};
    $obj->_set_root('Employee');
    $obj->clear_constraints;

    lives_ok {
        $obj->where(
            name => 'M*',
            age => [25 .. 35],
            age => {ne => 33},
            department => {lookup => 'Warehouse'},
            end => undef,
            Employee => {in => 'some-list'}
        );
    } "Can use dbix search syntax";

    my @constraints = $obj->constraints;

    # List and Ternary inherit from binary
    is (grep({$_->isa('Webservice::InterMine::Constraint::Binary')} @constraints), 4)
        or diag(explain [grep {$_->isa('Webservice::InterMine::Constraint::Binary')} @constraints]);
    my ($binary) = grep({$_->path eq 'Employee.name'} @constraints);
    is('M*', $binary->value);
    is('=', $binary->op);
    ($binary) = grep({$_->op eq '!='} @constraints);
    is('Employee.age', $binary->path);
    is(33, $binary->value);

    is (grep({$_->isa('Webservice::InterMine::Constraint::Unary')} @constraints), 1);
    my ($unary) = grep({$_->isa('Webservice::InterMine::Constraint::Unary')} @constraints);
    is("Employee.end", $unary->path);
    is('IS NULL', $unary->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::Multi')} @constraints), 1);
    my ($multi) = grep({$_->isa('Webservice::InterMine::Constraint::Multi')} @constraints);
    is("Employee.age", $multi->path);
    is_deeply([25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35], $multi->values);
    is('ONE OF', $multi->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::Ternary')} @constraints), 1);
    my ($ternary) = grep({$_->isa('Webservice::InterMine::Constraint::Ternary')} @constraints);
    is("Employee.department", $ternary->path);
    is('Warehouse', $ternary->value);
    is('LOOKUP', $ternary->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::List')} @constraints), 1);
    my ($list) = grep({$_->isa('Webservice::InterMine::Constraint::List')} @constraints);
    is("Employee", $list->path);
    is('some-list', $list->value);
    is('IN', $list->op);
}

sub chain_wheres_and_select:Test(22) {
    my $test = shift;
    my $obj  = $test->{object};
    $obj->_set_root('Employee');
    $obj->clear_constraints;
    $obj->clear_view;

    lives_ok {
        $obj->select('*', 'department.name')
            ->where(name => 'M*')
            ->where(age => [25 .. 35])
            ->where(age => {ne => 33})
            ->where(department => {lookup => 'Warehouse'})
            ->where(end => undef)
            ->where(Employee => {in => 'some-list'});
    } "Can use chained calls";

    is_deeply([$obj->views], [sort(qw/Employee.age Employee.end Employee.fullTime Employee.name/), 'Employee.department.name']);

    my @constraints = $obj->constraints;

    # List and Ternary inherit from binary
    is (grep({$_->isa('Webservice::InterMine::Constraint::Binary')} @constraints), 4)
        or diag(explain [grep {$_->isa('Webservice::InterMine::Constraint::Binary')} @constraints]);
    my ($binary) = grep({$_->path eq 'Employee.name'} @constraints);
    is('M*', $binary->value);
    is('=', $binary->op);
    ($binary) = grep({$_->op eq '!='} @constraints);
    is('Employee.age', $binary->path);
    is(33, $binary->value);

    is (grep({$_->isa('Webservice::InterMine::Constraint::Unary')} @constraints), 1);
    my ($unary) = grep({$_->isa('Webservice::InterMine::Constraint::Unary')} @constraints);
    is("Employee.end", $unary->path);
    is('IS NULL', $unary->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::Multi')} @constraints), 1);
    my ($multi) = grep({$_->isa('Webservice::InterMine::Constraint::Multi')} @constraints);
    is("Employee.age", $multi->path);
    is_deeply([25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35], $multi->values);
    is('ONE OF', $multi->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::Ternary')} @constraints), 1);
    my ($ternary) = grep({$_->isa('Webservice::InterMine::Constraint::Ternary')} @constraints);
    is("Employee.department", $ternary->path);
    is('Warehouse', $ternary->value);
    is('LOOKUP', $ternary->op);

    is (grep({$_->isa('Webservice::InterMine::Constraint::List')} @constraints), 1);
    my ($list) = grep({$_->isa('Webservice::InterMine::Constraint::List')} @constraints);
    is("Employee", $list->path);
    is('some-list', $list->value);
    is('IN', $list->op);
}

sub logic_after_adding_constraint : Test(1) {

    my $test = shift;
    my $obj  = $test->{object};
    $obj->clear_constraints;

    for my $age (1 .. 5) {
        $obj->add_constraint("Employee.age" => $age);
    }

    $obj->set_logic("(A and B) or (C and D) or E");

    $obj->add_constraint("Employee.name" => 6);

    is($obj->logic->code, "((A and B or C and D) or E) and F", "Adds an and'ed constraint to the logic");
}

sub logic : Test(7) {
    my $test = shift;
    my $obj  = $test->{object};
    my $a = $obj->add_constraint(
        path => 'Employee.name',
        op   => '=',
        value => 'Jo',
        code => 'A',
    );
    my $b = $obj->add_constraint(
        path => 'Employee.fullTime',
        op   => 'IS NOT NULL',
        code => 'B',
    );
    my $c = $obj->add_constraint(
        path => 'Employee.department.name',
        op   => '=',
        value => 'Sandwich Van',
        code => 'C',
    );
    is($obj->logic->code, $test->logic_string1, "Constructs default logic correctly");
    $obj->set_logic("A and (B or C)");
    is($obj->logic->code, 'A and (B or C)', "Constructs logic correctly from a string");
    $obj->set_logic(($a | $b) & $c);
    is($obj->logic->code, $test->logic_string2, "Constructs logic correctly from objects");
    throws_ok(
        sub {$obj->set_logic("A and Z")},
        qr/No constraint with code Z/,
        "catches wrong logic codes",
    );
    throws_ok(
        sub {$obj->set_logic("A foo B")},
        qr/unexpected element in logic string: foo/,
        "catches bad syntax",
    );
    throws_ok(
        sub {$obj->set_logic($a + $b)},
        qr/unexpected element in logic string/,
        "catches bad object syntax",
    );
    throws_ok(
        sub {$obj->set_logic($a | $test)},
        qr/does not pass the type constraint/,
        "catches bad objects",
    );
}

sub add_join : Test(5) {
    my $test = shift;
    my $obj  = $test->{object};
    lives_ok(sub {$obj->add_join('Employee.department.company.contractors')}, "Doesn't die adding a join");
    is(scalar($obj->all_joins), 1,"Can add a join ok");
    $obj->add_join('Employee.department.company.oldContracts');
    is(scalar($obj->all_joins), 2,"Can add another join ok");
    dies_ok(
        sub {$obj->add_join('Company.name')},
        "Catches inconsistent paths",
    );
    $obj->clear_joins;
    dies_ok(sub {$obj->add_join('Foo.bar')}, "Dies adding an invalid path");

}

sub view : Test(8) {
    my $test = shift;
    my $obj  = $test->{object};
    my @view = ('Employee.name', 'Employee.address.address', 'Employee.department.name');
    ok($obj->view_is_empty, "Reports empty view correctly");
    $obj->add_view(@view);
    is_deeply([$obj->views], \@view, "Can add valid views");
    ok(! $obj->view_is_empty, "Doesn't report non-empty view as empty");
    $obj->add_constraint(path => 'Employee', type => 'Manager');
    lives_ok(
        sub {$obj->add_view('Employee.title')},
        "is ok with subclassed paths"
    );
    is_deeply([$obj->views], [@view, 'Employee.title'], "View updated correctly");

    lives_ok {$obj->add_view('Employee.department.company.*')} "Can add views with wild cards";
    is_deeply([$obj->views], [@view, 'Employee.title', 'Employee.department.company.name', 'Employee.department.company.vatNumber']);

    dies_ok(sub {$obj->add_view('Foo', 'Bar', 'Baz.quux')}, "Dies adding bad views");
}

sub short_views : Test(4) {
    my $test = shift;
    my $obj  = $test->{object};
    $obj->_set_root('Employee');
    $obj->clear_view;

    lives_ok {$obj->add_view(qw/name age fullTime department.name/);} "Can age views without a head";

    is_deeply(
        [$obj->views],
        [qw/Employee.name Employee.age Employee.fullTime Employee.department.name/]
    );

    lives_ok {$obj->add_view('Employee.name')} "Lives adding full views";
    dies_ok {$obj->add_view('Department.name')} "Dies adding incompatible views";
}

sub testSelect : Test(6) {
    my $test = shift;
    my $obj  = $test->{object};
    $obj->_set_root('Employee');
    $obj->clear_view;

    lives_ok {$obj->select(qw/name age fullTime department.name/);} "Can use select";
    is_deeply(
        [$obj->views],
        [qw/Employee.name Employee.age Employee.fullTime Employee.department.name/]
    );
    lives_ok  {$obj->add_to_select(qw/address.address/);} "Can use add_to_select";
    is_deeply(
        [$obj->views],
        [qw/Employee.name Employee.age Employee.fullTime Employee.department.name Employee.address.address/]
    );
    lives_ok  {$obj->select(qw/name department.*/);} "using select again replaces the view";
    is_deeply(
        [$obj->views],
        [qw/Employee.name Employee.department.name/]
    );
}

sub sort_order_initial_state : Test {
    my $test = shift;
    my $obj  = $test->{object};
    dies_ok(sub {$obj->sort_order}, "Attempts to read sort order before it or view is set die");

}

sub sort_order : Test(6) {
    my $test = shift;
    my $obj  = $test->{object};
    my @view = ('Employee.name', 'Employee.address.address', 'Employee.department.name');
    $obj->add_view(@view);
    is($obj->sort_order, 'Employee.name asc', "And sets the default value correctly from the view");
    $obj->set_sort_order('Employee.department.name');
    is($obj->sort_order, 'Employee.department.name asc', "Updates path correctly");
    $obj->set_sort_order('Employee.department.name', 'desc');
    is($obj->sort_order, 'Employee.department.name desc', "Updates direction correctly");
    $obj->set_sort_order("Employee.age", "desc");
    is($obj->sort_order, "Employee.age desc", "Can set a relevant sort order that isn't in view");
    throws_ok(
        sub {$obj->set_sort_order('Employee.name', 'Around-and-Round')},
        qr/\(direction\) does not pass the type constraint/,
        "Dies setting good path with bad direction",
    );
    throws_ok(
        sub {$obj->set_sort_order('CEO.title')},
        qr/CEO.title/,
        "Dies setting sort order irrelevant to query",
    );
}

sub add_pathdescription : Test(3) {
    my $test = shift;
    my $obj  = $test->{object};
    is_deeply([$obj->path_descriptions], [], "Empty path_descriptions is an empty list");
    $obj->add_pathdescription(path => 'Employee.name', description => "The employee's name");
    is(scalar($obj->all_path_descriptions), 1, "Can add a path description");
    isa_ok($obj->path_descriptions->[0], 'Webservice::InterMine::PathDescription', "And it");
}

sub all_paths : Test(2) {
    my $test = shift;
    my $obj  = $test->{object};
    $obj->add_view('Employee.name');
    $obj->add_constraint(
	path => 'Employee.age',
	op   => 'IS NULL',
    );
    $obj->add_join('Employee.department.name');
    $obj->add_constraint(
	path => 'Employee',
	type => 'Manager',
    );

    is_deeply(
	[sort {$a cmp $b} $obj->all_paths],
	[sort {$a cmp $b} uniq($test->test_paths)],
	"Gets all the relevant paths for validation",
    )
	or diag(explain [sort {$a cmp $b} $obj->all_paths],
		explain [sort {$a cmp $b} uniq($test->test_paths)]);

    $obj->add_view('Employee');
    is_deeply(
        [sort {$a cmp $b} $obj->all_paths], 
        [sort {$a cmp $b} uniq($test->test_paths)], 
        "Duplicates are ignored",
    );
}

sub suspend_validation : Test(5) {
    my $test = shift;
    my $obj  = $test->{object};

    is($obj->is_validating, 1, "Has validation turned on by default");

    $obj->suspend_validation;

    is($obj->is_validating, 0, "... and can turn it off");

    lives_ok(
        sub {$obj->add_constraint(path => 'Foo', type => 'Quux');},
        "... so now it can add truly hideously incorrect paths and not die",
    );

    $obj->resume_validation;

    is($obj->is_validating, 1, "... but we can turn validation back on");
    dies_ok(sub {$obj->validate}, "... and it dies when we next call validate");
}

sub clean_out_SCCs : Test(4) {
    my $test = shift;
    my $obj  = $test->{object};
    my $starting_count = $obj->count_constraints;
    $obj->suspend_validation;
    $obj->add_constraint(
        path => 'Employee.age',
        type => 'Int',
    );
    $obj->add_constraint(
        path => 'Employee.name',
        type => 'Java.lang.string',
    );
    my $survivor = $obj->add_constraint(
        path => 'Employee',
        type => 'Manager',
    );
    is(
        $obj->count_constraints, $starting_count + 3,
        "Has the right number of constraints prior to clean out"
    );
    $obj->clean_out_SCCs;
    is(
        $obj->count_constraints, $starting_count + 1,
        "... but only good ones survive the purge",
    );
    ok(
        grep( {$_ eq $survivor} $obj->all_constraints),
        "... and the right one survived",
    );
    lives_ok(sub {$obj->validate}, "... and the object survives validation");
}

1;
