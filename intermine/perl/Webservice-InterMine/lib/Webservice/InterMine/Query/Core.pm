package Webservice::InterMine::Query::Core;

use Moose;
with(
    'Webservice::InterMine::Role::ModelOwner',
    'Webservice::InterMine::Role::Named',
    'Webservice::InterMine::Role::Described',
);

use Carp;
use List::Util qw/reduce/;
use List::MoreUtils qw/uniq/;

use MooseX::Types::Moose qw/Str Bool/;
use InterMine::TypeLibrary qw(
  PathList PathHash SortOrder
  ConstraintList LogicOrStr JoinList
  QueryName PathDescriptionList
  ConstraintFactory
);
use Webservice::InterMine::Join;
use Webservice::InterMine::PathDescription;
use Webservice::InterMine::Path qw(:validate);
use Webservice::InterMine::SortOrder;
use Exporter 'import';

our @EXPORT_OK = qw(AND OR);

######### ATTRIBUTES
has '+name' => ( isa => QueryName, );

has sort_order => (
    is      => 'ro',
    writer  => '_set_sort_order',
    isa     => SortOrder,
    lazy    => 1,
    coerce  => 1,
    trigger => sub {
        my $self = shift;
        $self->_validate;
    },
    default => sub {
        my $self = shift;
        ( $self->view )->[0];
    },
);

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
        add_view      => 'push',
        joined_view   => 'join',
        view_is_empty => 'is_empty',
        clear_view    => 'clear',
    },
);

after add_view => sub {
    my $self = shift;
    $self->_set_view( $self->joined_view(' ') );
};

after qr/^add_/ => sub {
    my $self = shift;
    $self->_validate;
};

has constraints => (
    traits     => ['Array'],
    is         => 'ro',
    isa        => ConstraintList,
    default    => sub { [] },
    auto_deref => 1,
    handles    => {
        all_constraints   => 'elements',
        push_constraint   => 'push',
        find_constraints  => 'grep',
        map_constraints   => 'map',
        delete_constraint => 'delete',
        count_constraints => 'count',
        clear_constraints => 'clear',
    },
);

sub get_constraint {
    my $self = shift;
    my $code = shift;
    confess "get_constraint needs one argument - "
      . "the code of the constraint you want - "
      . "and it must be one or two alphabetic characters"
      unless ( $code and $code =~ /^[A-Z]{1,2}$/ );
    my $criterion = sub { $_->code eq $code };
    my @matches = $self->find_constraints($criterion);
    if ( @matches > 1 ) {
        confess
"more than one constraint found - that should never happen. Please report this bug";
    }
    return $matches[0];
}

sub remove_constraint {
    my $self     = shift;
    my $delendum = shift;    # Constraintum delendum est
    my $i        = 0;
    for ( $self->all_constraints ) {
        if ( $_ eq $delendum ) {
            $self->delete_constraint($i);
        }
        $i++;
    }
}

sub coded_constraints {
    my $self      = shift;
    my $criterion = sub {
        $_->does('Webservice::InterMine::Constraint::Role::Operator');
    };
    return $self->find_constraints($criterion);
}

sub sub_class_constraints {
    my $self = shift;
    my $criterion =
      sub { $_->isa('Webservice::InterMine::Constraint::SubClass') };
    return $self->find_constraints($criterion);
}

sub constraint_codes {
    my $self = shift;
    return map { $_->code } $self->coded_constraints;
}

after push_constraint => sub {
    my $self = shift;
    $self->clear_logic;
};

sub type_dict {
    my $self = shift;
    my @sccs = $self->sub_class_constraints;
    my %type_dict;
    for (@sccs) {
        $type_dict{ $_->path } = $_->type;
    }
    return {%type_dict};
}

sub subclasses {
    my $self = shift;
    my @sccs = $self->sub_class_constraints;
    return map { $_->type } @sccs;
}

has joins => (
    traits     => ['Array'],
    is         => 'ro',
    isa        => JoinList,
    default    => sub { [] },
    auto_deref => 1,
    handles    => {
        all_joins   => 'elements',
        push_join   => 'push',
        map_joins   => 'map',
        clear_joins => 'clear',
    }
);

sub add_join {
    my $self = shift;
    my $join = Webservice::InterMine::Join->new(@_);
    $self->push_join($join);
    return $self;
}

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
    },
);

sub add_pathdescription {
    my $self = shift;
    my $pd   = Webservice::InterMine::PathDescription->new(@_);
    $self->push_path_description($pd);
    return $self;
}
has logic => (
    is      => 'rw',
    isa     => LogicOrStr,
    lazy    => 1,
    clearer => 'clear_logic',
    trigger => \&check_logic,
    default => sub {
        my $self = shift;
        reduce { $a & $b } $self->coded_constraints;
    },
);

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

sub all_paths {
    my $self    = shift;
    my $to_path = sub { $_->path };
    my @paths   = (
        $self->views,               $self->map_constraints($to_path),
        $self->map_joins($to_path), $self->map_path_descriptions($to_path),
    );
    return uniq(@paths);
}

############### METHODS

sub check_logic {
    my ( $self, $value ) = @_;
    unless ( blessed $value) {
        my $new_value = _parse_logic( $value, $self->coded_constraints );
        $self->logic($new_value);
    }
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
        } elsif ( $bit =~ /^[A-Z]+$/ ) {
            if ( $found_con{$bit} ) {
                push @processed_bits, '$found_con{' . $bit . '}';
            } else {
                confess "No constraint with code $bit in this query "
                  . " - we only have "
                  . join( ', ', keys %found_con );
            }
        } elsif ( $bit =~ /^and$/ ) {
            push @processed_bits, ' & ';
        } elsif ( $bit =~ /^or$/ ) {
            push @processed_bits, ' | ';
        } else {
            croak "unexpected element in logic string: $bit";
        }
    }
    return eval join '', @processed_bits;
}

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
    $self->push_constraint($constraint);
    return $constraint;
}

sub parse_constraint_string {
    if ( @_ > 1 ) {
        if (@_ % 2 == 0) {
            my %args = @_;
            my @keys = keys %args;
            if (    ( grep { $_ eq 'path' } @keys )
                and ( grep { $_ =~ /^(?:type|op)$/ } @keys ) ) {   
                return %args;  
            } 
        }
        my %args;
        @args{qw/path op value extra_value/} = @_;
        if ( ref $args{value} eq 'ARRAY' ) {
            $args{values} = delete $args{value};
        }
        return map { $_ => $args{$_} } grep { defined $args{$_} } keys(%args);
    } else {
        my $constraint_string = shift;
        my %args;
        my @bits = split /\s+/, $constraint_string, 2;
        if ( @bits < 2 ) {
            croak "can't parse constraint: $constraint_string";
        }
        $args{path} = $bits[0];
        $constraint_string = $bits[1];
        @bits = $constraint_string 
            =~ m/^
                (
                IS\sNOT\sNULL|
                IS\sNULL|
                NOT\sIN|\S+
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

sub clean_out_SCCs {
    my $self = shift;
    my $c;
    for ( $self->sub_class_constraints ) {
        if ( end_is_class( $self->model, $_->path ) ) {
            $self->remove_constraint($_);
            $c++;
        }
    }
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
    push @errs, $self->validate_paths;
    push @errs, $self->validate_sort_order;
    push @errs, $self->validate_subclass_constraints;
    push @errs, $self->validate_consistency;

    #   push @errs, $self->validate_logic;
    @errs = grep { $_ } @errs;
    croak join( '', @errs ) if @errs;
}

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

sub validate_sort_order {
    my $self = shift;
    return if $self->view_is_empty;
    unless ( grep { $self->sort_order->path eq $_ } $self->views ) {
        return $self->sort_order->path . " is not in the view\n";
    }
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
