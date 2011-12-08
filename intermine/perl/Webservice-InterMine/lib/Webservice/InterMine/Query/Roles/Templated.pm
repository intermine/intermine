package Webservice::InterMine::Query::Roles::Templated;

use Moose::Role;
use URI;
use List::MoreUtils qw/uniq zip/;

requires(qw/name description results _validate get_constraint show get_count print_results/);

# allows us to add this role to instances at run-time
use Moose::Meta::Class;
use MooseX::Types::Moose qw/Str/;

has [ 'comment', 'title', ] => (
    is      => 'rw',
    isa     => Str,
    default => '',
);

around 'to_string' => sub {
    my $orig = shift;
    my $self = shift;
    my $retval = $self->name;
    if (my $title = $self->title) {
        $retval .= " - $title";
    }
    if (my $desc = $self->description) {
        $retval .= "\n$desc";
    }
    return $retval unless ($retval eq $self->name);
    return $self->orig();
};

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

=head2 results_iterator_with(as => $row_format, %parameters)

Get an iterator over the rows of the result set. This is a more
memory efficient way to deal with large result sets that would
otherwise be difficult or impractical to hold in memory.

Parameters:
=over 4
=item as => tsv|csv|arrayrefs|hashrefs|jsonobjects|jsonrows|count: 
How each line should be returned.
=item size => Int: 
How many results to return (default: undef -> all)
=item start => Int: 
The index of the first result to return (default: 0)
=item addheaders => Bool/friendly/path: 
Whether to add headers to the output (default: false)
=item json => inflate|instantiate|perl: 
How to handle json (default: perl)
=item %parameters: 
The template parameters
=back

Returns: A L<Webservice::InterMine::ResultIterator> object.

=cut

sub results_iterator_with {
    my $self = shift;
    my %args = @_;
    my @keys = qw/as size start addheaders json/;
    my @values = delete(@args{@keys});
    my $clone = $self->get_adjusted_template(%args);
    return $clone->results_iterator(zip(@keys, @values));
}

=head2 results_with(as => $row_format, %parameters)

Get the results of the template back in the requested format.

Parameters:
=over 4
=item as => tsv|csv|arrayrefs|hashrefs|jsonobjects|jsonrows|count: 
How each line should be returned.
=item size => Int: 
How many results to return (default: undef -> all)
=item start => Int: 
The index of the first result to return (default: 0)
=item addheaders => Bool/friendly/path: 
Whether to add headers to the output (default: false)
=item json => inflate|instantiate|perl: 
How to handle json (default: perl)
=item %parameters: 
The template parameters
=back

Returns: An arrayref of rows in the requested format

=cut

sub results_with {
    my $self = shift;
    my %args = @_;
    my @keys = qw/as size start columnheaders json/;
    my @values = delete(@args{@keys});
    my $clone = $self->get_adjusted_template(%args);
    return $clone->results(zip(@keys, @values));
}

=head2 print_results_with(%template_values, %options)

Prints out the results of the query to the given file-handle
(or STDOUT if none is provided). This method is a convenience
method to be used when results should be saved directly to a file.

=cut

sub print_results_with {
    my $self = shift;
    my %args = @_;
    my @keys = qw/to as size start columnheaders json/;
    my @values = delete(@args{@keys});
    my $clone = $self->get_adjusted_template(%args);
    $clone->print_results(zip(@keys, @values));
}

=head2 show_with(%template_values, [to => $FH])

Print out a user-friendly view of the results, with the 
template values set to the given values, with the output
going to the provided file-handle, or STDOUT if none is 
provided.

=cut

sub show_with {
    my $self = shift;
    my %args = @_;
    my $to = delete($args{to});
    my $clone = $self->get_adjusted_template(%args);
    $clone->show($to);
}

=head2 get_count_with(%template_values)

Get the total number of result rows for the result
set for this template when run the given template
values.

=cut

sub get_count_with {
    my $self = shift;
    my $clone = $self->get_adjusted_template(@_);
    return $clone->get_count;
}

=head2 all_with(%template_values, %result_options);

Get all the rows of results for a query, given the specified 
options. This method explicitly removes any offset given and 
removes any size limit. Note that the server code limits result-sets
to 10,000,000 rows in size, no matter what.

=cut

sub all_with {
    my $self = shift;
    my %args = @_;
    my @keys = qw/as size start columnheaders json/;
    my @values = delete(@args{@keys});
    my $clone = $self->get_adjusted_template(%args);
    $clone->all(zip(@keys, @values));
}

=head2 first_with(%template_values, %result_options);

Return the first result (row or object). Any size options are ignored. May 
return undef if there are no results.

=cut

sub first_with {
    my $self = shift;
    my %args = @_;
    my @keys = qw/as size start columnheaders json/;
    my @values = delete(@args{@keys});
    my $clone = $self->get_adjusted_template(%args);
    $clone->first(zip(@keys, @values));
}

=head2 one_with(%template_values, %result_options);

Return one result (row or object), throwing an error if none or more than
one is received.

=cut

sub one_with {
    my $self = shift;
    my %args = @_;
    my @keys = qw/as size start columnheaders json/;
    my @values = delete(@args{@keys});
    my $clone = $self->get_adjusted_template(%args);
    $clone->one(zip(@keys, @values));
}

=head2 get_adjusted_template(%parameters)

Returns a clone of the current template with the values adjusted
to match those of the passed in template parameters.

=cut

sub get_adjusted_template {
    my $self = shift;
    my %template_parameters = @_;

    my $error_format = "'%s' is not a valid parameter to run_with";
    my (%new_value_for, %new_op_for, %new_extra_value_for);
    for ( keys %template_parameters ) {
        if (/^value/) {
            my ($code) = /^value([A-Z]{1,2})$/;
            confess sprintf( $error_format, $_ ) unless $code;
            $new_value_for{$code} = $template_parameters{$_};
        } elsif (/^op/) {
            my ($code) = /^op([A-Z]{1,2})$/;
            confess sprintf( $error_format, $_ ) unless $code;
            $new_op_for{$code} = $template_parameters{$_};
        } elsif (/^extra_value/) {
            my ($code) = /^extra_value([A-Z]{1,2})$/;
            confess sprintf( $error_format, $_ ) unless $code;
            $new_extra_value_for{$code} = $template_parameters{$_};
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
    $clone->set_logic($self->logic->code);
    return $clone;
}

around _validate => sub {
    my $orig = shift;
    my $self = shift;
    my @errs;
    push @errs, "Templates require a name attribute"
      unless $self->name;
    if ($self->count_constraints) {
        # Allow people to add views first.
        push @errs, "Invalid template: no editable constraints"
            unless $self->editable_constraints;
    }
    return $self->$orig(@errs);
};

    
1;
