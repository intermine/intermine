use strict;
use warnings;

use Test::Exception;
use Test::More tests => 18;

use Webservice::InterMine::ResultRow;

my @views = qw/
    Employee.name
    Employee.age
    Employee.department.name
    /;

my $data = [
    {value => "Tom"},
    {value => 33},
    {value => "Sales"},
];

my $row = Webservice::InterMine::ResultRow->new(views => \@views, cells => $data);

# Test index access
is $row->get_value(0), "Tom", "Can get by index";
is $row->get_value(1), 33, "... and another";
is $row->get_value(-1), "Sales", "... and negative index";
dies_ok {$row->get_value(3)} "Cannot get values out of range";
dies_ok {$row->get_value(-5)} "Cannot get values out of range";

# Test key access
is $row->get_value("Employee.name"), "Tom", "Can get by key";
is $row->get_value("Employee.age"), 33, "... and another";
is $row->get_value("department.name"), "Sales", "Can use aliases";
is $row->get_value("age"), 33, "... and another";

# Test to_aref
is_deeply ["Tom", 33, "Sales"], $row->to_aref, "Can get aref";

# Test to_href
is_deeply {
    "Employee.name" => "Tom",
    "name" => "Tom", 
    "Employee.age" => 33, 
    "age" => 33,
    "Employee.department.name" => "Sales",
    "department.name" => "Sales",
}, $row->to_href, "Can get href";

is_deeply {
    "name" => "Tom", 
    "age" => 33,
    "department.name" => "Sales",
}, $row->to_href("short"), "Can get short form href";

is_deeply {
    "Employee.name" => "Tom",
    "Employee.age" => 33, 
    "Employee.department.name" => "Sales",
}, $row->to_href("long"), "Can get long form href";

is("$row", "Employee\tname: Tom\tage: 33\tdepartment.name: Sales",
    "Can stringify");

#Test overloaded aref
is($row->[0], "Tom", "Can deref as array");
is_deeply ["Tom", 33, "Sales"], [@$row], "Can get aref";

# Test overloaded href
is($row->{name}, "Tom", "Can deref as hash");

is_deeply([$row->keys], [@views], "Can get views as keys");



