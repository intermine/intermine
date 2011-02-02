package Webservice::InterMine::Query::Roles::Templated;

use Moose::Role;
use URI;
use List::MoreUtils qw/uniq/;

requires(
    qw/name service description results _validate
      service_root templatequery_path get_constraint/
);

# allows us to add this role to instances at run-time
use Moose::Meta::Class;
use MooseX::Types::Moose qw/Str/;

has [ 'comment', 'title', ] => (
    is      => 'rw',
    isa     => Str,
    default => '',
);

sub type { 'template' }

sub insertion { {} }

sub head {
    my $self = shift;
    return {
        name            => $self->name,
        title           => $self->title,
        longDescription => $self->description,
        comment         => $self->comment,
    };
}

sub editable_constraints {
    my $self      = shift;
    my $criterion = sub {
        $_->is_editable
          and $_->does(
            'Webservice::InterMine::Constraint::Role::Operator');
    };
    return $self->find_constraints($criterion);
}

sub show_constraints {
    my $self = shift;
    my @editables = map { $_->code . ') ' . $_->to_string }
      sort { $a->code cmp $b->code } $self->editable_constraints;
    return join( "\n", @editables );
}

sub results_with {
    my $self   = shift;
    my %args   = @_;
    my $format = $args{as} and delete $args{as} if ( $args{as} );
    my %new_value_for;
    my %new_op_for;
    my %new_extra_value_for;
    my $error_format = "'%s' is not a valid parameter to run_with";
    for ( keys %args ) {
        if (/^value/) {
            my ($code) = /^value([A-Z]{1,2})$/;
            confess sprintf( $error_format, $_ ) unless $code;
            $new_value_for{$code} = $args{$_};
        } elsif (/^op/) {
            my ($code) = /^op([A-Z]{1,2})$/;
            confess sprintf( $error_format, $_ ) unless $code;
            $new_op_for{$code} = $args{$_};
        } elsif (/^extra_value/) {
            my ($code) = /^extra_value([A-Z]{1,2})$/;
            confess sprintf( $error_format, $_ ) unless $code;
            $new_extra_value_for{$code} = $args{$_};
        } else {
            confess sprintf( $error_format, $_ );
        }
    }
    my $clone = $self->clone;

    for my $code ( uniq( keys(%new_value_for), keys(%new_op_for) ) ) {
        if ( my $con = $clone->get_constraint($code) ) {
            confess
"You can only change values and operators for editable constraints"
              unless $con->is_editable;
            my %attr = %$con;
            $attr{value} = $new_value_for{$code}
              if ( exists $new_value_for{$code} );
            $attr{op} = $new_op_for{$code}
              if ( exists $new_op_for{$code} );
            $attr{extra_value} = $new_extra_value_for{$code} 
              if ( exists $new_extra_value_for{$code} );
            $clone->remove($con);
            $clone->add_constraint(%attr);
        } else {
            confess "no constraint with code $code on this query";
        }
    }

    my $results = $clone->results( as => $format );
    return $results;
}

sub url {
    my $self       = shift;
    my %args = @_;
    my $format = $args{format} || "tab";
    my $url        = $self->service_root . $self->templatequery_path;
    my $uri        = URI->new($url);
    my %query_form = (
        format => $format,
        name   => $self->name,
    );

    my $i = 1;
    for my $constraint ( $self->editable_constraints ) {
        next unless $constraint->switched_on;
        my %hash = $constraint->query_hash;
        while ( my ( $k, $v ) = each %hash ) {
            $query_form{ $k . $i } = $v;
        }
        $i++;
    }
    $uri->query_form(%query_form);
    warn $uri if $ENV{DEBUG};
    return $uri;
}

around _validate => sub {
    my $orig = shift;
    my $self = shift;
    my @errs;
    push @errs, "Templates require a name attribute"
      unless $self->name;
    push @errs, "Invalid template: no editable constraints"
      unless $self->editable_constraints;
    return $self->$orig(@errs);
};

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
    
    
1;
