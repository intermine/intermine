package Webservice::InterMine::Query::Core;

use Moose;
with(
    'Webservice::InterMine::Role::ModelOwner',
    'Webservice::InterMine::Role::Named',
    'Webservice::InterMine::Role::Described',
);

use Carp;
use List::Util qw/reduce/;
use List::MoreUtils qw/uniq natatime/;
use Scalar::Util qw/blessed/;
use Perl6::Junction qw/any/;

use MooseX::Types::Moose qw/Str Bool ArrayRef Undef/;
use Moose::Util::TypeConstraints qw(match_on_type);
use InterMine::Model::Types qw/PathList PathHash PathString ClassDescriptor/;
use Webservice::InterMine::Types qw(
  ListOperable Path CanTreatAsList
  SortOrder SortOrderList
  ConstraintList LogicOrStr JoinList
  QueryName PathDescriptionList
  ConstraintFactory
);

use Webservice::InterMine::Join;
use Webservice::InterMine::PathDescription;
use Webservice::InterMine::Path qw(:validate type_of);
use Webservice::InterMine::SortOrder;
use Exporter 'import';

our @EXPORT_OK = qw(AND OR);

######### ATTRIBUTES
has '+name' => ( 
    isa => QueryName, 
    coerce => 1,
);

=head2 root_path - The root of this query

The starting table for this query.

=head2 has_root_path - Whether or not this query has a root set

=cut

has root_path => (
    init_arg  => 'class', 
    isa       => PathString, 
    is        => 'ro',
    writer    => '_set_root',
    coerce    => 1,
    predicate => 'has_root_path',
    trigger    => sub {
        my ($self, $root) = @_;
        if ($self->is_validating) {
            my $err = validate_path( $self->model, $root, $self->type_dict );
            confess $err if $err;
        }
    },
);

=head2 path($expr) -> Path

Return a Webservice::InterMine::Path object corresponding to the
expression passed in (a dotted path string). The path will be made 
with the subclass information contained in the query.

=cut

sub path {
    my ($self, $str) = @_;
    @_ == 2 or croak "Expected one argument - a path expression, got: @_";
    # my @paths = $self->all_paths or croak "This query does not have any paths yet";
    $str = $self->prefix_path($str);
    # croak "$str is not a path in this query" unless (grep {$str eq $_} @paths);
    my $path = Webservice::InterMine::Path->new($str, $self, $self->type_dict);
    return $path;
}

=head2 has_sort_order - whether or not this query has a defined sort-order

=head2 push_sort_order - Add a sort order element to the sort order

=head2 sort_orders - get the list of sort orders

=head2 joined_so($sep) - Join the sort orders with a separator

=head2 clear_sort_order - delete all sort order information from the query

=head2 sort_order_is_empty - whether or not there are any elements in the sort order list

=cut

has _sort_order => (
    traits     => ['Array'],
    is         => 'ro',
    writer     => '_set_sort_order',
    isa        => SortOrderList,
    lazy_build => 1,
    coerce     => 1,
    predicate  => 'has_sort_order',
    trigger    => sub {
        my $self = shift;
        $self->_validate;
    },
    handles => {
        push_sort_order => 'push',
        sort_orders    => 'elements',
        joined_so      => 'join',
        clear_sort_order => 'clear',
        sort_order_is_empty =>  'is_empty',
        filter_sort_order => 'grep',
    },
);

=head2 sort_order - Return the string representing the sort order

=cut

sub sort_order {
    my $self = shift;
    confess "You can't use this method to modify this attribute"
        if shift;
    if (grep {not defined} $self->sort_orders) {
        return '';
    } elsif ($self->sort_orders == 0) {
        return $self->get_view(0) . ' asc';
    } else {
        return $self->joined_so(' ');
    } 
}

=head2 add_sort_order(@args) - add a sort order element to this query

=cut

sub add_sort_order {
    my $self = shift;
    my @args = @_;
    my $so = Webservice::InterMine::SortOrder->new(@args);
    $self->prefix_pathfeature($so);
    $self->push_sort_order($so);
}

=head2 order_by(@args) -> $self

Replace any existing sort order by the one defined with the given arguments. 
Return self to allow method chaining.

=cut

sub order_by {
    my ($self, @args) = @_;
    $self->clear_sort_order;
    my $it = natatime(2, @args);
    while (my @pair = $it->()) {
        $self->add_sort_order(@pair);
    }
    return $self;
}

=head2 prefix_pathfeature - prefix the path of a PathFeature with the query's root.

Used internally to process shortened, headless paths.

=cut

sub prefix_pathfeature {
    my ($self, $pf) = @_;
    my $new_path = $self->prefix_path($pf->path);
    $pf->set_path($new_path) unless ($new_path eq $pf->path);
}

=head2 prefix_path($path) -> a prefixed path

Used internally to process shortened, headless paths.

=cut

sub prefix_path {
    my ($self, $str) = @_;
    die "No path" unless $str;
    if ($self->has_root_path) {
        my $root = $self->root_path;
        my $new_path = ($str =~ /^$root/) ? $str : $self->root_path . ".$str";
        return $new_path;
    } else {
        my ($root) = split(/\./, $str);
        $self->_set_root($root);
        return $str;
    }
}

=head2 to_query

returns self to fulfil the Listable interface.

=cut

sub to_query {
    my $self = shift;
    return $self;
}

=head2 clone 

Return a clone of this query.

=cut

sub clone {
    my $self  = shift;
    my $clone = bless {%$self}, ref $self;
    $clone->{constraints} = [];
    $clone->suspend_validation;
    for my $con ($self->all_constraints) {
        $clone->add_constraint(%$con);
    }
    $clone->resume_validation;
    return $clone;
}
    

=head2 DEMOLISH

Called upon object destruction

=cut

sub DEMOLISH {
    my $self = shift;
    $self->suspend_validation;
}

sub _build__sort_order {
    my $self = shift;
    return $self->get_view(0);
}

=head2 set_sort_order(@elements) - replace any existing order with the given ones.

=cut

sub set_sort_order {
    my $self = shift;
    $self->_set_sort_order( join( ' ', @_ ) );
}

has view => (
    traits  => ['Array'],
    is      => 'ro',
    isa     => PathList,
    default => sub { [] },
    coerce  => 1,
    lazy    => 1,
    writer  => '_set_view',
    handles => {
        views         => 'elements',
        _add_views    => 'push',
        get_view      => 'get',
        joined_view   => 'join',
        view_is_empty => 'is_empty',
        clear_view    => 'clear',
        view_size     => 'count',
    },
);

=head2 view -> ArrayRef

Get the view as an array-ref

=head2 views -> list

Get the paths that make up the output columns (the view)

=head2 get_view($index) -> path

Get the view at the specified index

=head2 joined_view($sep) -> Str

Get a string consisting of the view paths joined with the given separator.

=head2 view_is_empty -> Bool

Return true if the view is currently empty.

=head2 clear_view

Clear the current view

=head2 view_size -> Int

Get the number of output columns

=head2 add_views(@views)

Add the given views to the view list, first preprending
the query's root, and checking for validity.

=head2 add_view

alias for add_views

=head2 add_to_select(@columns)

Alias for add_views

=head2 select(@columns)

Clear the current view and replace it with the given columns.

=cut

sub add_view {
    goto &add_views;
}

sub add_to_select {
    goto &add_views;
}

sub select {
    my $self = shift;
    $self->clear_view;
    $self->add_views(@_);
}

sub add_views {
    my $self = shift;
    my @views = map {$self->prefix_path($_)} map {split} @_;
    my @expanded_views;
    for my $view (@views) {
        if ($view =~ /(.*)\.\*$/) {
            my $path = $1;
            my $cd = $self->model->get_classdescriptor_by_name(type_of($self->model, $path));
            my @expanded = map { $path . '.' . $_->name } sort $cd->attributes;
            push @expanded_views, @expanded;
        } else {
            push @expanded_views, $view;
        }
    }
    $self->_add_views(@expanded_views);
    return $self;
}

after qr/^add_/ => sub {
    my $self = shift;
    $self->_validate;
};

=head2 constraints

Get the list of constraints in the order they were added. 
Returns an arrayref in scalar context,
and a list in list context.

=head2 all_constraints

Get all constraints as a list, in the order they were added.

=head2 map_constraints($coderef) -> list

Apply the coderef to each constraint in turn (ala C<map>)
and return the result of each call.

=head2 find_constraints($coderef) -> list

Apply the coderef to each constraint in turn (ala C<grep>) 
and return the constraints for which the code returns
a truthy value.

=head2 delete_constraint($index)

Delete the constraint with the given index from the list.

=head2 count_constraints -> Int

Get the number of constraints on this query.

=head2 clear_constraints 

Remove all constraints from this query

=cut

has constraints => (
    traits     => ['Array'],
    is         => 'ro',
    isa        => ConstraintList,
    default    => sub { [] },
    auto_deref => 1,
    handles    => {
        all_constraints   => 'elements',
        _push_constraint   => 'push',
        find_constraints  => 'grep',
        map_constraints   => 'map',
        delete_constraint => 'delete',
        _get_constraint   => 'get',
        count_constraints => 'count',
        clear_constraints => 'clear',
    },
);

sub get_constraint {
    my $self = shift;
    my $code = shift;
    confess "get_constraint needs one argument - "
      . "the code of the constraint you want - "
      . "and it must be one or two uppercase alphabetic characters,"
      . " not $code"
      unless ( $code and $code =~ /^[A-Z]{1,2}$/ );
    my $criterion = sub { 
        $_->does('Webservice::InterMine::Constraint::Role::Operator')
        &&
        $_->code eq $code 
    };
    my @matches = $self->find_constraints($criterion);
    if ( @matches > 1 ) {
        confess
"more than one constraint found - that should never happen. Please report this bug";
    }
    return $matches[0];
}

=head2 remove($constraint)

Remove the given object from the query, where the object is
a constraint child object (a path-description, a constraint or 
a join).

=cut

sub remove {
    my $self     = shift;
    my $delendum = shift;    # Constraintum delendum est
    my $i        = 0;
    my $type;
    for ($delendum->element_name) {
        if (/pathDescription/) {$type = 'path_description'}
        elsif (/join/)         {$type = 'join'}
        elsif (/constraint/)   {$type = 'constraint'}
        else {confess "Cannot delete elements of type $_ from queries"}
    }
    my $all = 'all_' . $type . 's';
    my $del = 'delete_' . $type;
    for ( $self->$all ) {
        if ( $_ eq $delendum ) {
            $self->$del($i);
        }
        $i++;
    }
}

=head2 remove_constraint($constraint | $code)

Remove the given constraint. If a string is passed instead, it is assumed
to be the code for this constraint, and the constraint with the given 
code is removed instead.

=cut

sub remove_constraint {
    my $self = shift;
    if (ref $_[0]) {
        $self->remove(@_);
    } else {
        my $code = shift;
        my $constraint = $self->get_constraint($code)
            or confess "No constraint with code $code!";
        $self->remove($constraint);
    }
}

=head2 coded_constraints -> list|Int

Return the constraints that have codes and can participate in logic. 
Returns a list in list context and the size of the list in scalar
context.

=cut

sub coded_constraints {
    my $self      = shift;
    my $criterion = sub {
        $_->does('Webservice::InterMine::Constraint::Role::Operator');
    };
    return $self->find_constraints($criterion);
}

=head2 sub_class_constraints -> list|Int

Return the constraints that constrain object types, and cannot participate in logic. 
Returns a list in list context and the size of the list in scalar
context.

=cut

sub sub_class_constraints {
    my $self = shift;
    my $criterion =
      sub { $_->isa('Webservice::InterMine::Constraint::SubClass') };
    return $self->find_constraints($criterion);
}

=head2 constraint_codes -> list

Return the codes (single characters from 'A' to 'Z') that this query
uses.

=cut

sub constraint_codes {
    my $self = shift;
    return map { $_->code } $self->coded_constraints;
}

after _push_constraint => sub {
    my $self = shift;
    if ($self->coded_constraints < 2 or not $self->has_logic) {
        return;
    }
    my $new_constraint = $self->_get_constraint(-1);
    unless ($new_constraint->isa('Webservice::InterMine::Constraint::SubClass') 
            or $self->count_constraints <= 1) {
        my $old_logic = $self->logic;
        $self->set_logic($old_logic & $new_constraint);
    }
};

=head2 type_dict -> hashref

returns a hashref with the mapping from class => subclass
for all constrained types within the query. This summarises the
information from subclass constraints.

=cut

sub type_dict {
    my $self = shift;
    my @sccs = $self->sub_class_constraints;
    my %type_dict;
    for (@sccs) {
        $type_dict{ $_->path } = $_->type;
    }
    return {%type_dict};
}

=head2 subclasses -> list

Return the list of subclasses as constrained in the query.

=cut

sub subclasses {
    my $self = shift;
    my @sccs = $self->sub_class_constraints;
    return map { $_->type } @sccs;
}

=head2 joins -> arrayref

Return an arrayref of the L<Webservice::InterMine::Join> objects
on this query, in the order they were added.

=head2 all_joins -> list

Returns the L<Webservice::InterMine::Join> objects of this query as a
list in the order they were added to the query.

=head2 map_joins($code) -> list

Apply the codereference to each join in the query in turn and 
return the results of the calls.

=head2 clear_joins

Remove all joins from the query

=head2 delete_join($index)

Remove the given join from the query.

=cut

has joins => (
    traits     => ['Array'],
    is         => 'ro',
    isa        => JoinList,
    default    => sub { [] },
    auto_deref => 1,
    handles    => {
        all_joins   => 'elements',
        _push_join   => 'push',
        map_joins   => 'map',
        clear_joins => 'clear',
        delete_join => 'delete',
    }
);

=head2 add_join( $path ) -> $self

Specifies the join style of a path on the query. 
The default join style this method adds is "OUTER", but
it can be specified with C<path =&gt; $path, style =&gt; $style>.
Possible join styles are INNER and OUTER.

=cut

sub add_join {
    my $self = shift;
    my $join = Webservice::InterMine::Join->new(@_);
    $self->prefix_pathfeature($join);
    $self->_push_join($join);
    return $self;
}

=head2 outerjoin($path) -> $self

A shortcut for C<< add_join($path, 'OUTER') >>.

=cut

sub outerjoin {
    my ($self, $path) = @_;
    confess "Too many arguments - 1 expected" if (@_ > 2);
    return $self->add_join($path, 'OUTER');
}

=head2 add_outer_join( $path )

A shortcut for C<< add_join($path, 'OUTER') >>.

=cut

sub add_outer_join {goto &outerjoin}

has path_descriptions => (
    traits     => ['Array'],
    is         => 'ro',
    isa        => PathDescriptionList,
    default    => sub { [] },
    auto_deref => 1,
    handles    => {
        all_path_descriptions => 'elements',
        push_path_description => 'push',
        map_path_descriptions => 'map',
        clear_path_descriptions => 'clear',
        delete_path_description => 'delete',
    },
);

sub add_pathdescription {
    my $self = shift;
    my $pd   = Webservice::InterMine::PathDescription->new(@_);
    $self->prefix_pathfeature($pd);
    $self->push_path_description($pd);
    return $self;
}

=head2 logic -> Logic

Returns the logic object for this query. This object stringifies to the
logic expression used in the serialisation. 

=head2 clear_logic 

Remove the current logic from this query

=head2 has_logic

Return true if this query has logic set for it.

=cut

has logic => (
    writer  => '_set_logic',
    reader  => 'logic',
    isa     => LogicOrStr,
    lazy    => 1,
    clearer => 'clear_logic',
    predicate => 'has_logic',
    trigger => \&_check_logic,
    default => sub {
        my $self = shift;
        reduce { $a & $b } $self->coded_constraints;
    },
);

=head2 set_logic($expr) -> self

Sets the logic for this query, validating it in the process.
Returns self to support chaining.

  $query->set_logic("A or B and C");

=cut

sub set_logic {
    my ($self, @args) = @_;
    $self->_set_logic(@args);
    return $self;
}

=head2 constraint_factory

The object responsible for making constraints for this query.

=cut

has constraint_factory => (
    is         => 'ro',
    isa        => ConstraintFactory,
    lazy_build => 1,
);

sub _build_constraint_factory {
    Webservice::InterMine::ConstraintFactory->new;
}

has is_validating => (
    traits  => ['Bool'],
    is      => 'ro',
    isa     => Bool,
    default => 1,
    handles => {
        suspend_validation => 'unset',
        resume_validation  => 'set',
    },
);

has is_dubious => (
    isa     => Bool,
    default => 0,
    is      => 'ro',
);
sub all_paths {
    my $self    = shift;
    my $to_path = sub { $_->path };
    my @paths   = (
        $self->views,               
        $self->map_constraints($to_path),
        $self->map_joins($to_path), 
        $self->map_path_descriptions($to_path),
    );
    return uniq(@paths);
}

sub all_children {
    my $self = shift;
    my @children;
    for my $meth (qw/all_path_descriptions all_joins all_constraints/) {
        push @children, $self->$meth;
    }
    return @children;
}
############### METHODS

sub _check_logic {
    my ( $self, $value ) = @_;
    unless ( blessed $value) {
        my $new_value = _parse_logic( $value, $self->coded_constraints );
        $self->set_logic($new_value);
    }
}

use Webservice::InterMine::LogicParser;

has logic_parser => (
    isa => 'Webservice::InterMine::LogicParser',
    is => 'ro',
    lazy_build => 1,
);

sub _build_logic_parser {
    my $self = shift;
    return Webservice::InterMine::LogicParser->new(query => $self);
}

sub _parse_logic {

   # eg: Organism_interologues: which has the fiercesome:
   # (B or G) and (I or F) and J and C and D and E and H and K and L and M and A
    my $logic_string = shift;
    my @cons         = @_;
    my %found_con;
    for my $con (@cons) {
        $found_con{ $con->code } = $con;
    }

    my @bits = split /\s?\b\s?/, $logic_string;
    my @processed_bits;

    for my $bit (@bits) {
        if ( $bit =~ /^[\(\)]$/ ) {
            push @processed_bits, $bit;
        }
        elsif ( $bit =~ /^[A-Z]+$/ ) {
            if ( $found_con{$bit} ) {
                push @processed_bits, '$found_con{' . $bit . '}';
            }
            else {
                confess "No constraint with code $bit in this query "
                  . " - we only have "
                  . join( ', ', keys %found_con );
            }
        }
        elsif ( $bit =~ /^and$/ ) {
            push @processed_bits, ' & ';
        }
        elsif ( $bit =~ /^or$/ ) {
            push @processed_bits, ' | ';
        }
        else {
            croak "unexpected element in logic string: $bit";
        }
    }
    return eval join '', @processed_bits;
}

=head2 add_constraint(@args) -> constraint

Adds a constraint corresponding to the given arguments, 
and returns the new constraint.

  $query->add_constraint('name', '=', 'foo');
  $query->add_constraint(age => {lt => 50});

=cut

sub add_constraint {
    my $self       = shift;
    my %args       = parse_constraint_string(@_);
    my $constraint = $self->constraint_factory->make_constraint(%args);
    if ( $constraint->can('code') ) {
        while ( grep { $constraint->code eq $_ } $self->constraint_codes ) {
            my $code = $constraint->code;
            $constraint->set_code( ++$code );
        }
    }
    $self->prefix_pathfeature($constraint);
    $self->_push_constraint($constraint);
    return $constraint;
}

=head2 search($constraints) -> self|results

Add the given constraints to the query, where those constraints 
are specified with an array-reference or a hash-reference. 
In list context return the result of the query. In scalar context, return
the query for chaining.

This method is similar in interface to the method of the same name 
in L<DBIx::Class::ResultSet>.

=cut

sub search {
    my $self = shift;
    my $constraints = shift;
    my $results_args = shift || {as => 'jsonobjects', json => 'instantiate'};
    if (ref $constraints eq 'HASH') {
        while (my ($path, $con) = each(%$constraints)) {
            $self->add_constraint($path, $con);
        }
    } elsif (ref $constraints eq 'ARRAY' and @$constraints % 2 == 0) {
        for (my $i = 0; $i < (@$constraints - 1); $i += 2) {
            my ($path, $con) = @{$constraints}[$i, ($i + 1)];
            $self->add_constraint($path, $con);
        }
    } else {
        croak "Bad argument: '$constraints' should be a hashref or an array ref with an even number of elements";
    }

    if (wantarray) {
        if ($self->view_is_empty and $self->has_root_path) {
            $self->select("*");
        }
        return $self->results(%$results_args);
    } else {
        return $self;
    }
}

=head2 where(@constraints) -> $self

Add the given constraints to the query, and return self to support chaining

  $query->where(
    name     => "foo",
    fullTime => 'true',
    age      => {gt => 10},
    'department.name'    => ['Sales', 'Accounting'],
    'department.company' => {lookup => 'Foo'},
  );

=cut

sub where {
    my $self = shift;
    if (@_ == 1) {
        return $self->search(@_);
    } else {
        return $self->search([@_]);
    }
}

=head2 parse_constraint_string

Interpret the constraint arguments so that constraints
can be constructed while allowing multiple representations of
constraints to be understood.

=cut

sub parse_constraint_string {
    confess "No arguments given!" unless @_;
    if ( @_ > 1 ) {
        if ( @_ % 2 == 0 ) {
            my %args = @_;
            my @keys = keys %args;
            if (    ( grep { $_ eq 'path' } @keys )
                and ( grep { $_ =~ /^(?:type|op)$/ } @keys ) )
            {
                return %args;
            }
        }
        if ( @_ == 2 ) {
            my ($path, $con) = @_;

            if (not defined $con) {
                return (path => $path, op => 'IS NULL');
            }
            unless (grep {$con eq $_} 'IS NULL', 'IS NOT NULL') {
                my %args;
                if (ref $con eq 'HASH') {
                    %args = (path => $path, %$con);
                } else {
                    %args = (path => $path);

                }

                my $value = $con;

                for my $key (keys %args) {
                    unless (grep {$key eq $_} qw/path op code extra_value type/) {
                        my $op = $key;
                        $value = delete($args{$op});
                        $args{op} = $op;
                    }
                }

                unless ($args{type}) {
                    match_on_type $value => (
                        ListOperable|CanTreatAsList, sub {
                            $args{op} ||= "IN";
                            $args{value} = $value;
                        },
                        ArrayRef, sub {
                            $args{op} ||= "ONE OF";
                            $args{values} = $value;
                        },
                        Undef, sub {
                            if (not defined $args{op} || $args{op} eq any('=', 'eq', 'is')) {
                                $args{op} = "IS NULL";
                            } elsif ($args{op} eq any('!=', 'ne', 'isnt')) {
                                $args{op} = "IS NOT NULL";
                            }
                        },
                        ClassDescriptor, sub {
                            $args{type} = $value;
                        },
                        Path, sub {
                            $args{op} ||= 'IS',
                            $args{loop_path} = $value;
                        },
                        sub {
                            $args{op} ||= '=';
                            $args{value} = $value;
                        }
                    );
                }
                return %args;
            }
        }

        my %args;
        @args{qw/path op value extra_value/} = @_;
        if ( ref $args{value} eq 'ARRAY' ) {
            $args{values} = delete $args{value};
        }
        return map { $_ => $args{$_} } grep { defined $args{$_} } keys(%args);
    }
    else {
        my $constraint_string = shift;
        my %args;
        my @bits = split /\s+/, $constraint_string, 2;
        if ( @bits < 2 ) {
            croak "can't parse constraint: $constraint_string";
        }
        $args{path} = $bits[0];
        $constraint_string = $bits[1];
        @bits = $constraint_string =~ m/^
                (
                IS\sNOT\sNULL|
                IS\sNULL|
                (?:NOT\s)?IN|\S+
                )
        	   (
               ?:\s+(.*)
               )?
	        /x;
        if ( @bits < 1 ) {
            croak "can't parse constraint: $constraint_string\n";
        }

        $args{op} = $bits[0];
        $args{value} = $bits[1] if $bits[1];
        return %args;
    }
}

=head2 clean_out_SCCs

Remove pointless subclass constraints from the query.

=cut

sub clean_out_SCCs {
    my $self = shift;
    for ( $self->sub_class_constraints ) {
        if ( end_is_class( $self->model, $_->path ) ) {
            $self->remove($_); # remove it because it is not a class
        } elsif (type_of($self->model, $_->path) eq $_->type) {
            $self->remove($_); # remove it because it is constraine to itself
        }
    }
}

=head2 to_string 

Return a readable representation of the query.

=cut

sub to_string {
    my $self = shift;
    my $ret = '';
    $ret .= 'VIEW: [' . $self->joined_view(', ') . ']';
    if ($self->constraints) {
        $ret .= ', CONSTRAINTS: [';
        for my $con ($self->constraints) {
            $ret .= '<' . $con->to_string . '>,';
        }
        $ret .= ']';
    }
    $ret .= ', LOGIC: ' . $self->logic->code if ($self->coded_constraints > 1);
    $ret .= ', SORT_ORDER: ' . $self->joined_so(' ');
    return $ret;
}

#########################
### VALIDATION

sub validate {    # called externally - forces validation
    my $self = shift;
    $self->resume_validation;
    $self->_validate;
}

sub _validate {    # called internally, obeys is_validating
    my $self = shift;
    return unless $self->is_validating;    # Can be paused, and resumed
    my @errs = @_;
    my @from_paths = $self->_get_from_paths();
    push @errs, $self->validate_paths;
    push @errs, $self->validate_sort_order(@from_paths);
    push @errs, $self->validate_subclass_constraints;
    push @errs, $self->validate_consistency;

    #   push @errs, $self->validate_logic;
    @errs = grep { $_ } @errs;
    confess join( '', @errs ) if @errs;
}

sub _get_from_paths {
    my $self = shift;
    my @from;
    push @from, map {s/\.[^\.]*$//; $_} $self->views;
    my @con_paths = $self->map_constraints(sub { $_->path});
    for my $cp (@con_paths) {
        if (my $p = eval {$self->path}) {
            if ($p->end_is_attribute) {
                push @from, $p->prefix->to_string;
            } else {
                push @from, $p->to_string;
            }
        }
    }
    return @from;
}

before _validate => sub {
    my $self = shift;
    if ($self->has_root_path and $self->view_is_empty) {
        $self->add_view('id');
    }
};

sub validate_paths {
    my $self = shift;
    my @paths = ( $self->all_paths, $self->subclasses );
    my @errs =
      map { validate_path( $self->model, $_, $self->type_dict ) } @paths;
    return @errs;
}

sub validate_consistency {
    my $self = shift;
    my @roots =
      map { root( $self->model, $_, $self->type_dict ) } $self->all_paths;
    unless ( uniq(@roots) == 1 ) {
        return
            "Inconsistent query: all paths must descend from the same root."
          . " - we got: "
          . join( ', ', map { $_->name } uniq @roots ) . "\n";
    }
    return undef;
}

sub is_in_view {
    my ($self, $path) = @_;
    return if $self->view_is_empty;
    return grep {$path eq $_} $self->views;
}

sub clean_out_irrelevant_sort_orders {
    my $self = shift;
    return if $self->view_is_empty;
    return if (not $self->has_sort_order or $self->sort_order_is_empty);
    my @from = $self->_get_from_paths;
    my @relevant_sos = $self->filter_sort_order(sub {
            any(@from) eq $self->path($_->path)->prefix->to_string});
    if (@relevant_sos) {
        $self->_set_sort_order(\@relevant_sos);
    } else {
        $self->clear_sort_order;
    }
    return;
}

sub validate_sort_order {
    my $self = shift;
    my @from_paths = @_;
    my @errors;
    return if $self->view_is_empty;
    if ($self->has_sort_order) {
        for my $so ($self->sort_orders) {
            my $p = eval { $self->path($so->path)};
            if ($p) {
                unless ( $p->prefix->to_string eq any(@from_paths) ) {
                    push @errors, sprintf(
                        "Order element %s refers to a class (%s) which isn't in the query\n",  
                        $so->path, $p->prefix->to_string);
                }
            } else {
                push @errors, sprintf("Order element path %s is invalid\n", $so->path);
            }
        }
    }
    return @errors if @errors;
    return;
}

sub validate_subclass_constraints {
    my $self = shift;
    my @errs;
    push @errs, map { end_is_class( $self->model, $_ ) }
      map { ( $_->path, $_->type ) } $self->sub_class_constraints;
    push @errs, map { b_is_subclass_of_a( $self->model, @$_ ) }
      map { [ $_->path, $_->type ] } $self->sub_class_constraints;
    return @errs;
}

# sub validate_logic {
#     my $self = shift;
#     my @errs;
#     my @constraints_in_logic = $self->logic->constraints;
#     my @constraints_in_query = $self->coded_constraints;
#     for my $con (@constraints_in_query) {
# 	unless (grep {$_ eq $con} @constraints_in_logic) {
# 	    push @errs, "Constraint " . $con->code . " is not in the logic (" .
# 		        $self->logic->code . ")\n";
# 	}
#     }
#     return @errs;
# }

########## DEPRECATED BITS

# Left in for backwards compatability

=head2 AND

=cut

sub AND {
    my ( $l, $r ) = @_;
    return $l & $r;
}

=head2 OR

=cut

sub OR {
    my ( $l, $r ) = @_;
    return $l | $r;
}
__PACKAGE__->meta->make_immutable;
no Moose;
1;
