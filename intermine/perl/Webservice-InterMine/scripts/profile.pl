package main;

use strict;
use warnings;

use Webservice::InterMine 'http://localhost:8080/intermine-test/service'; 

my @view = ('Employee.name', 'Employee.age', 'Employee.fullTime',
    'Employee.address.address', 'Employee.department.name',
    'Employee.department.company.name',
    'Employee.department.manager.name',
    );

my $service = Webservice::InterMine->get_service;

print "Webservice version: ", $service->version, "\n";

print "Model name: ", $service->model->model_name, "\n";

for (my $c = 0; $c++ < 100;) {
    my $q = $service->new_query;

    $q->add_view(@view);
    $q->add_constraint(
        path => 'Employee.age',
        op   => '>',
        value => 16,
    );

    $q->add_constraint(
        path => 'Employee.department',
        op   => 'IS NOT NULL',
    );

    $q->add_constraint(
        path => 'Employee.department',
        op   => 'LOOKUP',
        value => 'Sales'
    );

    $q->add_constraint(
        path => 'Employee.name',
        op   => 'ONE OF',
        values => [qw/Susan John Miguel/],
    );
    $q->add_constraint(
        path => 'Employee',
        type => 'CEO',
    );

    $q->set_sort_order('Employee.age', 'desc');

    $q = Webservice::InterMine->new_query;
    $q->add_view(@view);
    $q->add_constraint(
        path => "Employee.department.company",
        op => "LOOKUP",
        value => "CompanyA"
    );

    my $rows = $q->results;

    for my $row (@$rows) {
        printf("%s is %d years old, and lives at %s\n", @{$row}[0,1,3]);
    }

    $q->add_constraint(
        path  => 'Employee.age',
        op    => '<',
        value => 35,
    );

    my $rows = $q->results(as => "hashrefs");;

    for my $row (@$rows) {
        printf("%s is %d years old, and lives at %s\n", 
            @{$row}{qw/Employee.name Employee.age Employee.address.address/});
    }

    my $records = $q->results(as => 'jsonobjects', json => 'perl');

    for my $record (@$records) {
        printf("%s is %d years old, and lives at %s\n", 
            $record->{name}, $record->{age}, $record->{address}{address});
    }

    print $q->results(as => 'jsonobjects', json => 'raw'), "\n";

    $records = $q->results(as => 'jsonobjects', json => 'inflate');

    for my $record (@$records) {
        printf("%s is %d years old, and lives at %s\n", 
            $record->name, $record->age, $record->address->address);
    }

    $records = $q->results(as => 'jsonobjects', json => 'instantiate');

    for my $record (@$records) {
        printf("%s is %d years old, and lives at %s\n", 
            $record->getName, $record->getAge, $record->getAddress->getAddress);
    }

    my $t = Webservice::InterMine->template('employeesFromCompanyAndDepartment');

    $rows = $t->results_with(valueA => "CompanyB");

    for my $row (@$rows) {
        printf("%s is %d years old\n", @$row);
    }

    $rows = $t->results(as => "jsonobjects");

    for my $row (@$rows) {
        printf("%s is a %d year-old %s\n", $row->{name}, $row->{age}, $row->{class});
    }
}

