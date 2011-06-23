package Test::Webservice::InterMine::Query::Core;

use strict;
use warnings;
use base qw(Test::Class);
use List::MoreUtils qw(uniq);
use Test::More;
use Test::Exception;
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
    lives_ok {$q = $test->class->new($test->args)} "Can make a query"
        or diag(explain [$test->args]);
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
    my @methods = (
	qw/
	      name description sort_order set_sort_order view add_view
	      constraints add_constraint push_constraint find_constraints
	      map_constraints coded_constraints parse_constraint_string
	      type_dict subclasses joins all_joins push_join map_joins add_join
	      path_descriptions all_path_descriptions push_path_description
	      map_path_descriptions add_pathdescription logic all_paths
	      validate validate_paths validate_sort_order
	      validate_subclass_constraints
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

sub add_constraint : Test(20) {
    my $test = shift;
    my $obj  = $test->{object};
    my @cons;
    lives_ok(
	sub {
	    push @cons, $obj->add_constraint(
		path => 'Employee.fullTime',
		op   => 'IS NOT NULL',
		$test->extra_constraint_args,
	    );
	},
	"makes a unary constraint without dying",
    ) or diag explain $test;
    isa_ok($cons[-1], 'Webservice::InterMine::Constraint::Unary', ".. and it");
    lives_ok(
	sub {
	    push @cons, $obj->add_constraint(
		path => 'Employee.age',
		op   => '<',
		value => 16,
		$test->extra_constraint_args,
	    );
	},
	"makes a binary constraint without dying",
    );
    isa_ok($cons[-1], 'Webservice::InterMine::Constraint::Binary', ".. and it");
    lives_ok(
	sub {
	    push @cons, $obj->add_constraint(
		path => 'Employee.department',
		op   => 'LOOKUP',
		value => 'Post Room',
		extra_value => 'Woolworths',
		$test->extra_constraint_args,
	    );
	},
	"makes a ternary constraint with extra_value without dying",
    );
    isa_ok($cons[-1], 'Webservice::InterMine::Constraint::Ternary', ".. and it");
    lives_ok(
	sub {
	    push @cons, $obj->add_constraint(
		path => 'Employee.address',
		op   => 'LOOKUP',
		value => '14 Mill Lane',
		$test->extra_constraint_args,
	    );
	},
	"makes a ternary constraint without extra_value without dying",
    );
    isa_ok($cons[-1], 'Webservice::InterMine::Constraint::Ternary', ".. and it");
    lives_ok(
	sub {
	    push @cons, $obj->add_constraint(
		path => 'Employee',
		type => 'Manager',
		$test->extra_constraint_args,
	    );
	},
	"makes a subclass constraint without dying",
    );
    isa_ok($cons[-1], 'Webservice::InterMine::Constraint::SubClass', ".. and it");
    lives_ok(
	sub {
	    push @cons, $obj->add_constraint(
		path => 'Employee.title',
		op   => 'ONE OF',
		values => [qw/Lacky Lickspittle Dogsbody/],
		$test->extra_constraint_args,
	    );
	},
	"makes a multi constraint on a subclassed path without dying",
    ) or diag explain $obj->type_dict;
    isa_ok($cons[-1], 'Webservice::InterMine::Constraint::Multi', ".. and it");
    lives_ok(
	sub {
	    push @cons, $obj->add_constraint('Employee.department.company.vatNumber IS NULL');
	},
	"makes a unary from parsing without dying",
    );
    isa_ok($cons[-1], 'Webservice::InterMine::Constraint::Unary', ".. and it");
    lives_ok(
	sub {
	    push @cons, $obj->add_constraint('Employee.department.company.address.address = "UK"');
	},
	"makes a binary from parsing without dying",
    );
    isa_ok($cons[-1], 'Webservice::InterMine::Constraint::Binary', ".. and it");
    $obj->clear_constraints;
    dies_ok(sub {$obj->add_constraint('Foo.bar IS NULL')}, "Dies adding a constraint with an invalid path");
    $obj->clear_constraints;
    dies_ok(
	sub {$obj->add_constraint(path => 'Manager', type => 'Employee')},
	"Dies adding an invalid subclass constraint - not a subclass"
    );
    $obj->clear_constraints;
    dies_ok(
	sub {$obj->add_constraint(path => 'Employee', type => 'Quux')},
	"Dies adding an invalid subclass constraint - bad class"
    );
    $obj->clear_constraints;
    $obj->add_constraint('Employee.name = "Foo"');
    throws_ok(
	sub {$obj->add_constraint('Company.name = "Bar"')},
	qr/Inconsistent query/,
	"Dies adding an inconsistent constraint",
    );
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
    throws_ok(
	sub {$obj->add_join('Company.name')},
	qr/Inconsistent query/,
	"Catches inconsistent paths",
    );
    $obj->clear_joins;
    dies_ok(sub {$obj->add_join('Foo.bar')}, "Dies adding an invalid path");

}

sub view : Test(6) {
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
    dies_ok(sub {$obj->add_view('Foo', 'Bar', 'Baz.quux')}, "Dies adding bad views");
}

sub sort_order_initial_state : Test {
    my $test = shift;
    my $obj  = $test->{object};
    dies_ok(sub {$obj->sort_order}, "Attempts to read sort order before it or view is set die");

}

sub sort_order : Test(5) {
    my $test = shift;
    my $obj  = $test->{object};
    my @view = ('Employee.name', 'Employee.address.address', 'Employee.department.name');
    $obj->add_view(@view);
    is($obj->sort_order, 'Employee.name asc', "And sets the default value correctly from the view");
    $obj->set_sort_order('Employee.department.name');
    is($obj->sort_order, 'Employee.department.name asc', "Updates path correctly");
    $obj->set_sort_order('Employee.department.name', 'desc');
    is($obj->sort_order, 'Employee.department.name desc', "Updates direction correctly");
    throws_ok(
	sub {$obj->set_sort_order('Employee.name', 'Around-and-Round')},
	qr/\(direction\) does not pass the type constraint/,
	"Dies setting good path with bad direction",
    );
    throws_ok(
	sub {$obj->set_sort_order('CEO.title')},
	qr/CEO.title is not in the view/,
	"Dies setting path not in the view",
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
