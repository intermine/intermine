package TestPathFeature;

use base qw(Test::Class);
use Test::More;
use Test::Exception;

my $module = 'InterMine::PathFeature';
my %args   = (
    path => 'Some.path.here',
);

sub loader : Test(startup => 1) {
    require_ok( $module );
}

sub object : Test(setup) {
    my $self   = shift;
    my $object = $module->new(%args);
    $self->{object} = $object;
}

sub methods : Test {
    can_ok($module, (qw/path to_string to_hash/));
}

sub attributes : Test {
    my $self   = shift;
    my $object = $self->{object};
    dies_ok(
	sub {$object->path('Some.other.path')},
	'Dies attempting to change path',
    );
}

sub strict_construction : Test(3) {
    dies_ok(
	sub {$module->new()},
	'Dies constructing with too few args',
    );
    dies_ok(
	sub {$module->new(%args, foo => 'bar')},
	'Dies constructing with extraneous args',
    );
    new_ok($module => [%args]);
}

1;
