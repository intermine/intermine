package Test::Webservice::InterMine::Query;

use strict;
use warnings;
use Carp qw/confess/;

use base ('Test::Webservice::InterMine::Query::Core');
sub class {'Webservice::InterMine::Query'}
sub args {my $test = shift; return ('fake.url');}
sub def_view { return ('Employee.name', 'Employee.address.address', 'Employee.department.name');}
sub xml {
q|<query constraintLogic="A and B and C" model="testmodel" name="" sortOrder="Employee.name asc" view="Employee.name Employee.address.address Employee.department.name">
   <pathDescription description="The name of the employee" pathString="Employee.name"/>
   <join path="Employee.name" style="OUTER"/>
   <constraint code="A" op="=" path="Employee.department.name" value="Sandwich Distribution"/>
   <constraint code="B" op="&lt;" path="Employee.age" value="18"/>
   <constraint code="C" op="ONE OF" path="Employee.name">
     <value>Tom</value>
     <value>Dick</value>
     <value>Harry</value>
   </constraint>
   <constraint path="Employee" type="Manager"/>
 </query>|
}

sub empty_xml {q|<query model="testmodel" name="" sortOrder="Employee.name asc" view="Employee.name Employee.address.address Employee.department.name"/>|}
sub modern_url {return q|FAKEROOTFAKEPATH|}
sub legacy_url {return q|FAKEROOTFAKEPATH|}

use Test::More;
use Test::Exception;
use Test::XML;
use Test::MockObject::Extends;
use Test::MockObject;

use Webservice::InterMine::Service;
use Webservice::InterMine::ResultIterator;

sub startup : Test(startup => 3) {
    my $test = shift;

    my $service = Test::MockObject->new;
    $service->fake_module(
	'Webservice::InterMine::Service',
        new => sub {
            return $service;
        },
    );
    $service->set_isa('Webservice::InterMine::Service');
    $service->mock(
        model => sub {
            return $test->model;
        },
    );

    $service->mock(
        root => sub {
            return 'FAKEROOT';
        },
    );
    $service->mock(
        version => sub {2},
    );
    $service->mock(
        QUERY_PATH => sub {
            return 'FAKEPATH';
        },
    );
    $test->{service} = $service;

    my $iterator = Test::MockObject::Extends->new('Webservice::InterMine::ResultIterator');
    $iterator->mock(
        get_all => sub {
            my $self = shift;
            return $self->row_format;
        },
    );
    $test->{iterator} = $iterator;
    $test->SUPER::startup;
}


sub setup : Test(setup) {
    my $test = shift;
    $test->SUPER::setup;
    my $obj = $test->class->new($test->args);
    $obj->add_view($test->def_view);
    $obj->add_constraint(
        path => 'Employee.department.name',
        op => '=',
        value => 'Sandwich Distribution',
        code => 'A',
    );
    $obj->add_constraint(
        path => 'Employee.age',
        op => '<',
        value => 18,
        code => 'B',
    );
    $obj->add_constraint(
        path => 'Employee.name',
        op => 'ONE OF',
        values => [qw/Tom Dick Harry/],
        code => 'C',
    );
    $obj->add_constraint(
        path => 'Employee',
        type => 'Manager',
    );
    $obj->add_join('Employee.name');
    $obj->add_pathdescription(
        path => 'Employee.name',
        description => 'The name of the employee',
    );
    $test->{filled_obj} = $obj;
}

sub _methods : Test(2) {
    my $test = shift;
    $test->SUPER::_methods;
    my @methods = (
	qw/url results results_iterator to_xml service_root/
    );
    can_ok($test->class, @methods);
}

sub _inheritance : Test(3) {
    my $test = shift;
    my $parent = 'Webservice::InterMine::Query::Core';
    isa_ok($test->class, $parent, "Inherits ok -");
    my @roles = (qw/
        Webservice::InterMine::Query::Roles::Runnable
        Webservice::InterMine::Query::Roles::WriteOutAble
    /);
    for (@roles) {
        ok($test->class->does($_), "... and does $_");
    }
}

sub service_methods : Test(2) {
    my $test = shift;
    my $obj = $test->{filled_obj};
    my $service = $test->{service};
    $service->mock(
        get_results_iterator => sub {
            my $self = shift;
            return @_;
        },
    )->mock(
        version => sub {2},
    );
    is_deeply(
        [$obj->results_iterator],
        [
            $test->modern_url,
            {
                query => '<query view="Employee.name Employee.address.address Employee.department.name" name="" model="testmodel" sortOrder="Employee.name asc" constraintLogic="A and B and C"><pathDescription pathString="Employee.name" description="The name of the employee"/><join style="OUTER" path="Employee.name"/><constraint value="Sandwich Distribution" path="Employee.department.name" code="A" op="="/><constraint value="18" path="Employee.age" code="B" op="&lt;"/><constraint path="Employee.name" code="C" op="ONE OF"><value>Tom</value><value>Dick</value><value>Harry</value></constraint><constraint type="Manager" path="Employee"/></query>'
            },
            [$test->def_view],
            'rr',
            'perl',
            undef,
        ],
        "... and results iterator likewise",
    );
    is_deeply(
        [$obj->results_iterator(with => [qw/a b c/])],
        [
            $test->modern_url,
            {
                query => '<query view="Employee.name Employee.address.address Employee.department.name" name="" model="testmodel" sortOrder="Employee.name asc" constraintLogic="A and B and C"><pathDescription pathString="Employee.name" description="The name of the employee"/><join style="OUTER" path="Employee.name"/><constraint value="Sandwich Distribution" path="Employee.department.name" code="A" op="="/><constraint value="18" path="Employee.age" code="B" op="&lt;"/><constraint path="Employee.name" code="C" op="ONE OF"><value>Tom</value><value>Dick</value><value>Harry</value></constraint><constraint type="Manager" path="Employee"/></query>'
            },
            [$test->def_view],
            'rr',
            'perl',
            ['a', 'b', 'c'],
        ],
        "... and results iterator likewise with roles",
    );
}

sub to_xml : Test(3) {
    my $test = shift;
    my $obj = $test->{filled_obj};
    is_xml($obj->to_xml, $test->xml, "Serialises to xml ok");
    $obj = $test->{object};
    dies_ok(
	sub {$obj->to_xml},
	"dies trying to serialise without at least a view defined",
    );
    $obj->add_view($test->def_view);
    is_xml($obj->to_xml, $test->empty_xml, "Serialises empty queries ok");
}

sub url : Test(2) {
    my $test = shift;
    my $obj  = $test->{filled_obj};
    is($obj->url, $test->modern_url, "Makes a good url");
    my $service = $test->{service};
    $service->mock(
	version => sub {
	    return 1;
	},
    );
    is($obj->url, $test->legacy_url, "Makes a good legacy url");
}

# Test that results passed the right parameters up the food-chain
sub results : Test(4) {
    my $test = shift;
    my $obj  = $test->{filled_obj};
    my $service = $test->{service};
    $service->mock(
	get_results_iterator => sub {
        sub MockedResIt::get_all {return shift}
        my $self = shift;
        my $args = [@_];
        return bless $args, 'MockedResIt';
	});

    is_deeply(
        $obj->results(as => 'string'), 
        [
            'FAKEROOTFAKEPATH', 
                {
                query => '<query view="Employee.name Employee.address.address Employee.department.name" name="" model="testmodel" sortOrder="Employee.name asc" constraintLogic="A and B and C"><pathDescription pathString="Employee.name" description="The name of the employee"/><join style="OUTER" path="Employee.name"/><constraint value="Sandwich Distribution" path="Employee.department.name" code="A" op="="/><constraint value="18" path="Employee.age" code="B" op="&lt;"/><constraint path="Employee.name" code="C" op="ONE OF"><value>Tom</value><value>Dick</value><value>Harry</value></constraint><constraint type="Manager" path="Employee"/></query>'
            },
            [$test->def_view],
            'tab',
            'perl',
            undef,
        ],
        "returns new-line joined string for string results",
    );
    is_deeply(
        $obj->results(as => 'arrayref'),
        [
            'FAKEROOTFAKEPATH', 
                {
                query => '<query view="Employee.name Employee.address.address Employee.department.name" name="" model="testmodel" sortOrder="Employee.name asc" constraintLogic="A and B and C"><pathDescription pathString="Employee.name" description="The name of the employee"/><join style="OUTER" path="Employee.name"/><constraint value="Sandwich Distribution" path="Employee.department.name" code="A" op="="/><constraint value="18" path="Employee.age" code="B" op="&lt;"/><constraint path="Employee.name" code="C" op="ONE OF"><value>Tom</value><value>Dick</value><value>Harry</value></constraint><constraint type="Manager" path="Employee"/></query>'
            },
            [$test->def_view],
            'arrayref',
            'perl',
            undef,
        ],
        "returns array ref of arrayrefs for arrayref results",
    );
    is_deeply(
        $obj->results(),
        [
            'FAKEROOTFAKEPATH', 
                {
                query => '<query view="Employee.name Employee.address.address Employee.department.name" name="" model="testmodel" sortOrder="Employee.name asc" constraintLogic="A and B and C"><pathDescription pathString="Employee.name" description="The name of the employee"/><join style="OUTER" path="Employee.name"/><constraint value="Sandwich Distribution" path="Employee.department.name" code="A" op="="/><constraint value="18" path="Employee.age" code="B" op="&lt;"/><constraint path="Employee.name" code="C" op="ONE OF"><value>Tom</value><value>Dick</value><value>Harry</value></constraint><constraint type="Manager" path="Employee"/></query>'
            },
            [$test->def_view],
            'rr',
            'perl',
            undef,
        ],
        "Defaults to result-row"
    );
}

sub path_method_modification:Test(2) {
    my $test = shift;
    my $obj = $test->{object};
    $obj->add_view("Employee.name");
    my $path = $obj->path("name");
    is ("$path", "Employee.name");
    isa_ok($path->{service}, 'Webservice::InterMine::Service');
}

1;
