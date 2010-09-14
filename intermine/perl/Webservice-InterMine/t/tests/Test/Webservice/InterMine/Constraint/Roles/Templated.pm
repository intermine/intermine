package Test::Webservice::InterMine::Constraint::Roles::Templated;

use Test::More;
use Test::Exception;

sub role { 'Webservice::InterMine::Constraint::Role::Templated' }

sub make_template_object {
    my $test       = shift;
    my @args       = @_;
    my $meta_class = $test->meta_class;
    return $meta_class->new_object(@args);
}

sub template_attributes {(
    is_editable	 => 1,
    is_locked	 => 1,
    switched_on	 => 1,
    identifier	 => undef,
    is_writable => 0,
)}

sub meta_class {
    my $test  = shift;
    my $class = $test->class;
    return $test->{metaclass} if defined $test->{metaclass};
    use_ok($class);
    $test->{metaclass} = $class->meta->create_anon_class(
	superclasses => [$class],
	roles        => [$test->role],
	cache        => 1,
    );
    return $test->{metaclass};
}

sub test_template_inheritance {
    my $test = shift;
    ok(
	$test->{object}->DOES($test->role),
	"... and does " . $test->role,
    );
}

sub test_template_construction {
    my $test = shift;
    my @baseargs = @_;
    my $type = $test->type;
    $i = 1;
    for ($test->good_template_args) {
	lives_ok(
	    sub{ $test->make_object(@baseargs, @$_)},
	    "... succeeds building a $type template constraint with arg set " . $i++,
	);
	ok(
	    $test->make_object(@baseargs, @$_)->DOES($test->role),
	    "... and the $type constraint does " . $test->role,
	);
    }
    $i = 1;
    for ($test->bad_template_args) {
	dies_ok(
	    sub{ $test->make_object(@baseargs, @$_)},
	    "... dies building a $type template constraint with illegal arg set " . $i++ .' ('. join(', ', @$_) .')',
	);
    }
}

sub test_template_methods {
    my $test = shift;
    my @template_methods = (qw/
	is_editable switched_on is_locked query_hash switchable_string
	identifier hide unhide lock unlock
    /);
    can_ok($test->{object}, @template_methods);
}

sub template_hash_elements {
    return (
	editable   => 'true',
	switchable => 'locked',
	identifier => undef,
	description => undef,
    );
}

sub template_string_suffix {
    ' (locked)'
}

sub good_template_args {
    return (
	[is_editable => 1],
	[is_editable => 0],
	[is_editable => 1, is_locked => 0],
	[is_editable => 1, is_locked => 1],
	[is_editable => 1, is_locked => 1, switched_on => 1],
	[is_editable => 1, is_locked => 0, switched_on => 1],
	[is_editable => 1, is_locked => 0, switched_on => 0],
	[is_editable => 1, is_locked => 0, switched_on => 0, identifier => 'foo'],
    );
}

sub bad_template_args {
    return (
	[is_editable => 0, is_locked => 0],
	[is_editable => 0, is_locked => 0, switched_on => 1],
	[is_editable => 0, is_locked => 0, switched_on => 0],
	[is_editable => 0, is_locked => 0, switched_on => 1],
	[is_editable => 1, switched_on => 0],
	[switched_on => 0],
	[is_locked => 0],
	[is_locked => 0, switched_on => 1],
    );
}
1;
