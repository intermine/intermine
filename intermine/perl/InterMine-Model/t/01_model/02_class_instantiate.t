#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 3;
use Test::Exception;

use InterMine::Model;

my $model = InterMine::Model->new(file => 't/data/testmodel_model.xml');


subtest "Test instantiation" => sub {
    plan tests => 5;
    note "Testing instantiation";

    lives_ok {$model->make_new("Company")} "Can make objects";

    my $emp;
    lives_ok {$emp = $model->make_new(Employee => (name => "John", age => 24))} 
        "Can make an object with a list";

    is($emp->getName, "John", "who has the right name");

    lives_ok {$emp->setDepartment($model->make_new("Department", {name => "Sales"}))} 
        "Can make an object with a hashref";

    is $emp->getDepartment->getName, "Sales", "Which has the right name";
};

subtest "Test field type constraints" => sub {
    note "Testing field type constraints";
    plan tests => 9;
    my $types = $model->make_new("Types");

    subtest "Test bool types" => sub {
        note "\ntesting booleans";
        plan tests => 6;
        throws_ok {$types->setBooleanType("Foo")} qr/Validation failed for 'Bool'/,
            "and it throws errors when you try to put something into the wrong slot";
        throws_ok {$types->setBooleanObjType("Foo")} qr/Validation failed for 'Bool'/,
            "and it throws errors when you try to put something into the wrong slot";
        lives_ok {$types->setBooleanType(1)} "Can set bool to true";
        ok($types->getBooleanType, "And it is set correctly");
        lives_ok {$types->setBooleanObjType(0)} "Can set bool to false";
        ok(! $types->getBooleanObjType, "And it is set correctly");
    };

    subtest "Test float types" => sub {
        note "\ntesting floats";
        plan tests => 6;
        throws_ok {$types->setFloatType("Foo")} qr/Validation failed for 'Num'/,
            "and it throws errors when you try to put something into the wrong slot";
        throws_ok {$types->setFloatObjType("Foo")} qr/Validation failed for 'Num'/,
            "and it throws errors when you try to put something into the wrong slot";

        lives_ok {$types->setFloatType(1.123)} "Can set float to 1.123";
        is($types->getFloatType, 1.123, "And it is set correctly");
        lives_ok {$types->setFloatObjType(1.123)} "Can set float to 1.123";
        is($types->getFloatObjType, 1.123, "And it is set correctly");
    };

    subtest "Test double types" => sub {
        note "\ntesting doubles";
        plan tests => 6;
        throws_ok {$types->setDoubleType("Foo")} qr/Validation failed for 'Num'/,
            "and it throws errors when you try to put something into the wrong slot";
        throws_ok {$types->setDoubleObjType("Foo")} qr/Validation failed for 'Num'/,
            "and it throws errors when you try to put something into the wrong slot";

        lives_ok {$types->setDoubleType(1.123)} "Can set double to 1.123";
        is($types->getDoubleType, 1.123, "And it is set correctly");
        lives_ok {$types->setDoubleObjType(1.123)} "Can set double to 1.123";
        is($types->getDoubleObjType, 1.123, "And it is set correctly");
    };

    subtest "Test long types" => sub {
        note "\ntesting longs";
        plan tests => 9;

        my $error = qr/Validation failed for 'InterMine::TypeLibrary::BigInt' with value Foo/;
        throws_ok {$types->setLongType("Foo")}  $error, 
            "and it throws errors when you try to put something into the wrong slot"
                or diag($types->getLongType);
        throws_ok {$types->setLongObjType("Foo")} $error,
            "and it throws errors when you try to put something into the wrong slot"
                or diag($types->getLongObjType);

        my $attr = $types->meta->get_attribute("longType");
        ok($attr->should_coerce, "This attribute will coerce");
        my $tc = $attr->type_constraint;

        my $coerced = $tc->coerce("9_223_372_036_854_775_807");
        is(ref($coerced), "Math::BigInt", "... and it can coerce");
        ok($coerced == "9_223_372_036_854_775_807", "... correctly");

        lives_ok {$types->setLongType("9_223_372_036_854_775_807")} 
            "Can set long to 9_223_372_036_854_775_807 (nine pentillion)";
        ok($types->getLongType == Math::BigInt->new("9_223_372_036_854_775_807"), 
            "And it is set correctly");
        lives_ok {$types->setLongObjType("-4_123_456_789")} 
            "Can set long to -4_123_456_789 (negative 4 billion)";
        ok($types->getLongObjType == Math::BigInt->new("-4_123_456_789"), 
            "And it is set correctly");
    };

    subtest "Test short types" => sub {
        note "\ntesting shorts";
        plan tests => 6;
        throws_ok {$types->setShortType("0.1")} qr/Validation failed for 'Int'/,
            "and it throws errors when you try to put something into the wrong slot";
        throws_ok {$types->setShortObjType("0.1")} qr/Validation failed for 'Int'/,
            "and it throws errors when you try to put something into the wrong slot";

        lives_ok {$types->setShortType(1_123)} "Can set short to 1,123";
        is($types->getShortType, 1_123, "And it is set correctly");
        lives_ok {$types->setShortObjType(1_123)} "Can set short to 1,123";
        is($types->getShortObjType, 1_123, "And it is set correctly");
    };

    subtest "Test integer types" => sub {
        note "\ntesting ints";
        plan tests => 6;
        throws_ok {$types->setIntType("0.1")} qr/Validation failed for 'Int'/,
            "and it throws errors when you try to put something into the wrong slot";
        throws_ok {$types->setIntObjType("0.1")} qr/Validation failed for 'Int'/,
            "and it throws errors when you try to put something into the wrong slot";

        lives_ok {$types->setIntType(1_123)} "Can set int to 1,123";
        is($types->getIntType, 1_123, "And it is set correctly");
        lives_ok {$types->setIntObjType(1_123)} "Can set int to 1,123";
        is($types->getIntObjType, 1_123, "And it is set correctly");
    };

    subtest "Test string types" => sub {
        note "\ntesting strings";
        plan tests => 3;
        throws_ok {$types->setStringObjType(["Some String"])} qr/Validation failed for 'Str'/,
            "and it throws errors when you try to put something into the wrong slot";

        lives_ok {$types->setStringObjType("Foo")} 'Can set string to "Foo"';
        is($types->getStringObjType, "Foo", "And it is set correctly");
    };

    subtest "Test reference types" => sub {
        note "\ntesting references";
        plan tests => 3;
        my $emp = $model->make_new("Employee");
        throws_ok {$emp->setDepartment($model->make_new("Company"))} 
            qr/Validation failed for 'Department' with value Company/,
            "and it throws errors when you try to put something into the wrong slot";
        lives_ok {
            $emp->setDepartment($model->make_new("Department"));
            $emp->getDepartment->setName("Sales");
        } "Can set a ref";
        is $emp->getDepartment->getName, "Sales", "And it is set correctly";
    };

    subtest "Test collection types" => sub {
        note "\ntesting collections";
        plan tests => 7;
        my $dep = $model->make_new("Department");
        my $emp1 = $model->make_new("Employee");
        my $emp2 = $model->make_new("Employee");
        my $emp3 = $model->make_new("Employee");
        my $manager = $model->make_new("Manager");
        my $manager2 = $model->make_new("Manager");
        my $contractor = $model->make_new("Contractor");
        my $company = $model->make_new("Company");
        my $error = qr/Validation failed for 'ArrayOfEmployee'/;
        throws_ok {$dep->setEmployees([$contractor])} $error,
            "Complains about lists of the wrong type";
        throws_ok {$dep->setEmployees([$emp1, $company])} $error,
            "even when some of the items are ok";

        lives_ok {$dep->setEmployees([$emp1, $emp2, $manager])}
            "Is ok heterogeneous lists which include subtypes though";

        is_deeply [$dep->getEmployees], [$emp1, $emp2, $manager],
            "The list elements are all set ok";

        lives_ok {$dep->addEmployee($emp3)}
            "Can add a single obj to the list";
        lives_ok {$dep->addEmployee($manager2)}
            "Can add a subtyped obj to the list";

        is_deeply [$dep->getEmployees], [$emp1, $emp2, $manager, $emp3, $manager2],
            "The list elements are all set ok";
    };
};

subtest "Test type coercion" => sub {
    note "\ntesting coercion";

    my $dep = $model->make_new("Department", {name => "Sales"});

    lives_ok {$dep->setCompany({name => "FooCorp"})} "Can coerce a ref from a hashref";

    my $emp2;
    lives_ok {$emp2 = $model->make_new(
            Employee => name => "John", age => 24, department => {name => "HR"});}
            "Coercion works within new";

    is($emp2->getDepartment->getName, "HR", "And correctly too");

    my $emp3;
    lives_ok {
        $emp3 = $model->make_new(
            Employee => (
                name => "John", 
                age => 24, 
                department => {
                    name => "HR", 
                    company => {
                        name => "Bar Inc.",
                        contractors => [
                            {name => "Janet"},
                            {name => "Bernie"},
                        ],
                    }
                }
            ));
        } "Nested Coercion works within new";

    is($emp3->getDepartment->getCompany->getName, "Bar Inc.", "And it was coerced correctly");
    is($emp3->getDepartment->getCompany->getContractors->[1]->getName, "Bernie",
        "... including collections");

    lives_ok {$dep->setEmployees([{name => "John"}, {name => "Jane", class => "Manager"}])}
        "Can coerce a list of hashrefs to objects";
    lives_ok {$dep->addEmployee({name => "Simon"})} 
        "Can coerce from a hashref when adding to a collection";
    lives_ok {$dep->addEmployee({name => "Fred", class => "Manager"})} 
        "Can coerce a subclass from a hashref when adding to a collection";


    subtest "Can loop over collections" => sub {
        note "\ntesting dereferencing of collections";
        my @expected_name = qw/John Jane Simon Fred/;
        my @expected_class = qw/Employee Manager Employee Manager/;
        my $i = 0;
        for my $e ($dep->getEmployees) {
            is($e->getName, $expected_name[$i], "This employee is called $expected_name[$i]");
            is($e->class->name, $expected_class[$i], "This employee isa $expected_class[$i]");
            $i++;
        }
    };
};
