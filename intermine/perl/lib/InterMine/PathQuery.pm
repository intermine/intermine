package InterMine::PathQuery;

=head1 NAME

InterMine::PathQuery - an object representation of a query

=head1 SYNOPSIS

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::PathQuery

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2009 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use strict;
use Carp qw/croak carp confess/;

use base qw(InterMine::ModelOwner);

use InterMine::Path;
use InterMine::PathQuery::Constraint;

use IO::String;
use XML::Writer;

use Exporter 'import';

our @EXPORT_OK = qw(AND OR);

=head2 new

 Usage   : my $path_query = new InterMine::PathQuery($model);
 Function: create a new, empty path query
 Args    : $model - the InterMine::Model to use for validating paths in the
                    query

=cut
sub new {
  my $class = shift;

  if (@_ != 1) {
    croak "PathQuery::new() needs 1 argument - the model\n";
  }

  my $model = shift;

  croak "Invalid model" unless ($model->isa('InterMine::Model'));

  my $self = {view => [], next_code => 'A'};

  bless $self, $class;
  $self->model($model);
  return $self;
}

=head2 add_view

 Usage   : $path_query->add_view("Department.name Department.company.name");
             or
           $path_query->add_view(qw(Department.name Department.company.name));
 Function: add paths to the "view" of this PathQuery so that they will appear
           in the output
 Args    : $paths - the paths to add, either a string of space or comma
                    separated paths, or list of paths

=cut
sub add_view
{
  my $self = shift;

  if (@_ == 0) {
    croak "no arguments passed to add_view()\n";
  }

  my @paths = map { split /[,\s]+/ } @_;

  for my $path (@paths) {
    InterMine::Path->validate($self->{model}, $path);

    if (!grep {$_ eq $path} @{$self->{view}}) {
      push @{$self->{view}}, $path;
    }
  }
}

=head2 view or get_views

 Usage   : my @view_paths = $path_query->view();
       or: my @view_paths = $path_query->get_views();
 Function: get the current view paths

=cut
sub view
{
  my $self = shift;
  croak 'No view set for query' unless ($self->{view});
  
  return @{$self->{view}};
}

sub get_views {return shift->view}; # alias to view


=head2 sort_order

 Usage   : $path_query->sort_order('Department.name');
 Function: set the sort order
 Args    : $path - a path from the current view that will be the new sort order

=cut

sub sort_order {
  my ($self, $sort_order) = @_;

  if ($sort_order) {
      $self->{sort_order} = $sort_order;
  }

  return $self->{sort_order};
}
sub _validate_has_view {
    my $self = shift;
    croak "Query ", $self->get_name, " is not valid: No view set" unless $self->view;
}
sub _validate_views {
    my $self = shift;
    for my $path ($self->view) {
	croak qq("$path" is not a valid Path for this model.) 
	    unless (InterMine::Path->validate($self->model, $path, $self->type_hash));
    }
}

sub _validate_sort_order {
    my $self = shift;
    return 1 unless $self->{sort_order};
    my ($so) = split(/\s/, $self->{sort_order});
    my ($is_in_here,@query_cds, $class);
    if ($so) {
   	my @bits = InterMine::Path->validate($self->model, $so, $self->type_hash);
	$class = $bits[-2] || $bits[-1];
	$is_in_here = grep {$_ eq $class} 
	                   map {InterMine::Path->validate($self->model, $_, $self->type_hash)} 
	                       $self->get_views, $self->get_node_paths;;
    }	
    else {
	$is_in_here++;
    }  
    croak ('Invalid sort order: "', $self->{sort_order},'"-"', $class->name, '" is not in the query')
     	unless $is_in_here;
    return $is_in_here;
}

=head2 get_sort_order

 Title   : get_sort_order()
 Usage   : $sort_order = $template->get_sort_order();
 Function: Get the path by which the results for the
           query will be sorted
 Args    : no args
 Returns : a string

=cut

sub get_sort_order {
    my $self = shift;
    return $self->sort_order;
}
sub has_sort_order {
    my $self = shift;
    return defined $self->sort_order;
}

=head2 add_constraint

 Usage   : $path_query->add_constraint("Department.name = '$dep_name'");
 Function: add a constraint to this query
 Args    : $constraint - the constraint description
 Returns : an InterMine::PathQuery::Constraint object.

=cut
sub add_constraint
{
  my $self = shift;
  my $arg = shift;

  if (!defined $arg) {
    croak "no constraint string specified for PathQuery->add_constraint()\n";
  }

  my ($c, $path);

  # Allow constraints to be passed straight from templates to PathQueries
  if (ref $arg eq 'InterMine::PathQuery::Constraint') {
      $c = $arg;
      $path = $c->get_path;
  }
  else {
      my @bits = split /\s+/, $arg, 2;
      if (@bits < 2) {
	  croak "can't parse constraint: $arg\n";
      }

      $path = $bits[0];
      
      croak qq("$path" is not a valid Path for this model.\n) 
	  unless (InterMine::Path->validate($self->{model}, $path));

      my $constraint_string = $bits[1];
      
      $c = new InterMine::PathQuery::Constraint($constraint_string);
      $c->_set_path($path); # So the constraint knows what it's doing, and is able
                        # To use the as_string method nicely.
  }

  push @{$self->{constraints}->{$path}}, $c;

  return $c;
}


=head2 get_all_constraints

 Title   : get_all_constraints()
 Usage   : @constraints = $query->get_all_constraints;
 Function: get a list of all the constraints in the associated PathQuery.
 Args    : no args
 Returns : an array of InterMine::PathQuery::Constraint objects
 
=cut


sub get_all_constraints {
    my $self = shift;
    my @constraints = ();

    for my $constraints_aref (values %{$self->{constraints}}) {
	push @constraints, @$constraints_aref;
    }
    return @constraints;

}

sub _check_logic_arg
{
  my $arg = shift;
  if (not ref $arg or ref $arg !~ /^InterMine::PathQuery::Constraint(?:Set)$/) {
    my $message = "the argument must be a Constraint or ConstraintSet object";
    if (ref $arg) {
      $message .= ", not class: " . ref $arg;
    }
    croak "$message\n";
  }
}

# Left in for backwards compatability
=head2 AND

=cut
sub AND {
    _check_logic_arg($_) for (@_);
    my ($l, $r) = @_;
    return $l & $r;
}

=head2 OR

=cut

sub OR {
    _check_logic_arg($_) for (@_);
    my ($l, $r) = @_;
    return $l | $r;
}


=head2 get_logic

 Title   : get_logic()
 Usage   : $logic = $template->get_logic();
 Function: Get the a string representation of the logic 
           behind the query.
 Args    : no args
 Returns : a string

=cut

sub get_logic {
    my $self = shift;
    return $self->logic;
}

sub has_logic {
    my $self = shift;
    return defined $self->logic;
}

=head2 logic

=cut
sub logic
{
  my $self = shift;

  if (@_ > 1) {
    croak("Too many arguments to logic() (max 1): you gave me:", 
	  join(', ', map {'"'.$_.'"'} @_) )
  }

  my $logic = shift;
  if ($logic) { # ConstraintSet->new will catch passing in ConstraintSet objs
      if (ref $logic and $logic->isa('InterMine::PathQuery::ConstraintSet')) {
	  $self->{logic} = $logic;
      }
      else {
	  $self->{logic} = $self->_parse_logic($logic);
      }
  }
  if ( ! defined $self->{logic} and defined $self->{constraintLogic}) {
      return $self->logic($self->{constraintLogic});
  }
  if (defined $self->{logic}) {
      croak "Invalid logic: not a ConstraintSet" 
	  unless $self->{logic}->isa('InterMine::PathQuery::ConstraintSet');
  }
  return $self->{logic};
}


sub _parse_logic {
   # eg: Organism_interologues: which has the fiercesome:
   # (B or G) and (I or F) and J and C and D and E and H and K and L and M and A

    my $self = shift;
    my $constraint_string = shift;

    my %found_con;
    for my $con ($self->get_all_constraints) {
	$found_con{$con->code} = $con;
    }

    my @bits = split /\s?\b\s?/, $constraint_string;
    my @processed_bits;

    for my $bit (@bits) {
	if ($bit =~ /^[\(\)]$/) {
	    push @processed_bits, $bit;
	}
	elsif ($bit =~ /^[A-Z]+$/) {
	    if ($found_con{$bit}) {
		push @processed_bits, '$found_con{'.$bit.'}';
	    }
	    else {
		croak "No constraint with code $bit in this query";
	    }
	}
	elsif ($bit =~ /^and$/) {
	    push @processed_bits, ' & ';
	}
	elsif ($bit =~ /^or$/) {
	    push @processed_bits, ' | ';
	}
	else {
	    croak "unexpected element in logic string: $bit";
	}
    }

    return eval join '', @processed_bits;
}

=head2

 Usage   : $path_query->to_xml_string()
 Function: return an XML representation of this path query

=cut
sub to_xml_string {my $self = shift; return $self->to_query_xml};
sub to_query_xml {
    my $self = shift;
    $self->_is_valid('die_on_error');

    my $output = IO::String->new();
    my $writer = XML::Writer->new(DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);

    my %start_tag_args = (
	name      => $self->get_name, 
	model     => $self->model->model_name,
	view      => (join ' ', $self->get_views),
	);
    $start_tag_args{longDescription} = $self->get_description if $self->has_description;
    $start_tag_args{constraintLogic} = $self->logic->as_string if $self->has_logic;
    $start_tag_args{sortOrder} = $self->get_sort_order if $self->has_sort_order;

    $writer->startTag('query', %start_tag_args);

    for my $path_string ($self->get_described_paths) {
	$writer->startTag( 'pathDescription', 
			   pathString  => $path_string,
			   description => $self->get_description_for($path_string),
	    );
	$writer->endTag();
    }

    for my $path_string ($self->get_node_paths) {
	
	my $path = InterMine::Path->new($self->model, $path_string, $self->type_hash);
	my $type = $self->type_of($path_string) || $path->end_type;
	
	# Write the tag
	$writer->startTag(
	    'node', 
	    path => $path_string, 
	    type => $type,
	    );

	for my $constraint ($self->get_constraints_on($path_string)) {
	    my %con_tags = $constraint->get_xml_tags;
	    $writer->startTag('constraint', %con_tags);
	    $writer->endTag();
	}
	$writer->endTag();
    }

    $writer->endTag();

    return ${$output->string_ref};
}


sub get_node_paths {
    my $self = shift;
    return sort keys %{$self->{constraints}};
}

sub get_constraints_on {
    my $self = shift;
    my $node = shift;

    return @{$self->{constraints}{$node}};
}

sub get_described_paths {
    my $self = shift;
    return sort keys %{$self->{pathDescriptions}};
}

sub get_description_for {
   my $self = shift;
   my $path = shift;
   
   return $self->{pathDescriptions}{$path};
}


sub _is_valid
{
  my $self = shift;
  my $die_on_error = shift;
  $self->_validate_has_view;
  $self->_validate_sort_order;
  $self->_validate_views;
}

=head2 get_name

 Title   : get_name()
 Usage   : $name = $query->get_name();
 Function: Get the unique name for the query.
 Args    : no args
 Returns : a string

=cut

sub get_name {
    my $self = shift;
    return $self->{name} || '';
}

sub set_name {
    my ($self, $name) = @_;
    croak "No value supplied to set_name" unless $name;
    return $self->{name} = $name;
}

=head2 get_description

 Title   : get_description()
 Usage   : $description = $query->get_description();
 Function: Get a human readable informative description of what this
           query does
 Args    : no args
 Returns : a string

=cut

sub get_description {
    my $self = shift;
    my $desc = $self->{longDescription};
    return $desc;
}

sub has_description {
    my $self = shift;
    return defined $self->get_description;
}

sub _has_view_path
{
  my $self = shift;
  my $path = shift;

  return grep {$_ eq $path} @{$self->{view}};
}

sub type_hash { 
    my $self = shift;
    my $new_hash = shift;
    if ($new_hash) {
	croak "type hash must be a hash reference" unless (ref $new_hash eq 'HASH');
	$self->{type_of} = $new_hash;
    }
    return $self->{type_of};
}

sub type_of {
    my ($self, $key, $value) = @_;
    if ($value) {
	$self->{type_of}{$key} = $value;
    }
    return $self->{type_of}{$key};
}

1;
