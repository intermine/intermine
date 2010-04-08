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

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use strict;

use InterMine::Path;
use IO::String;
use XML::Writer;

use InterMine::PathQuery::Constraint;

use Exporter;

our @ISA = qw(Exporter);
our @EXPORT_OK = qw(AND OR);

=head2 new

 Usage   : my $path_query = new InterMine::PathQuery($model);
 Function: create a new, empty path query
 Args    : $model - the InterMine::Model to use for validating paths in the
                    query

=cut
sub new
{
  my $class = shift;

  if (@_ != 1) {
    die "PathQuery::new() needs 1 argument - the model\n";
  }

  my $model = shift;

  my $self = {model => $model, view => [], next_code => 'A'};

  return bless $self, $class;
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
    die "no arguments passed to add_view()\n";
  }

  my @paths = map { split /[,\s]+/ } @_;

  for my $path (@paths) {
    InterMine::Path->validate($self->{model}, $path);

    if (!grep {$_ eq $path} @{$self->{view}}) {
      push @{$self->{view}}, $path;
    }
  }
}

=head2 view

 Usage   : my @view_paths = $path_query->view();
 Function: get the current view paths

=cut
sub view
{
  my $self = shift;
  return @{$self->{view}};
}

=head2 sort_order

 Usage   : $path_query->sort_order('Department.name');
 Function: set the sort order
 Args    : $path - a path from the current view that will be the new sort order

=cut
sub sort_order
{
  my $self = shift;

  if (@_ == 0) {
    my $sort_order = $self->{sort_order};
    if (defined $sort_order && $self->_has_view_path($sort_order)) {
      return $sort_order;
    } else {
      # the sort path has gone from the view or was never set, find another
      my @view = $self->view();
      if (@view) {
        $self->{sort_order} = $view[0];
        return $view[0];
      } else {
        die "can't get the sort order because the view is not set\n";
      }
    }
  } else {
    my $sort_order = shift;

    if ($self->_has_view_path($sort_order)) {
      $self->{sort_order} = $sort_order;
    } else {
      die "the new sort order ($sort_order) is not in the view (",
          $self->view(), "\n";
    }
  }
}

=head2 add_constraint

 Usage   : $path_query->add_constraint("Department.name = '$dep_name'");
 Function: add a constraint to this query
 Args    : $constraint - the constraint description

=cut
sub add_constraint
{
  my $self = shift;
  my $arg = shift;

  if (!defined $arg) {
    die "no constraint string specified for PathQuery->add_constraint()\n";
  }

  my @bits = split /\s+/, $arg, 2;
  if (@bits < 2) {
    die "can't parse constraint: $arg\n";
  }

  my $path = $bits[0];

  InterMine::Path->validate($self->{model}, $path);

  my $constraint_string = $bits[1];

  my $c = new InterMine::PathQuery::Constraint($constraint_string);

  $c->code($self->{next_code}++);

  push @{$self->{constraints}->{$path}}, $c;

  return $c;
}

sub _check_logic_arg
{
  my $arg = shift;
  if (not ref $arg or ref $arg !~ /^InterMine::PathQuery::Constraint(?:Set)$/) {
    my $message = "the argument must be a Constraint or ConstraintSet object";
    if (ref $arg) {
      $message .= ", not class: " . ref $arg;
    }
    die "$message\n";
  }
}

sub _operator_implementation
{
  my $op = shift;

  if (@_ < 2) {
    die "not enough arguments to $op operator\n";
  }

  map {_check_logic_arg($_)} @_;

  return bless {op => $op, constraints => [@_]}, 'InterMine::PathQuery::ConstraintSet';

}

=head2 AND

=cut
sub AND
{
  return _operator_implementation('and', @_);
}

=head2 OR

=cut
sub OR
{
  return _operator_implementation('or', @_);
}

=head2 logic

=cut
sub logic
{
  my $self = shift;

  if (@_ != 1) {
    die "logic() needs one argument\n";
  }

  my $logic = shift;

  _check_logic_arg($logic);

  $self->{logic} = $logic;
}

sub _logic_string
{
  my $arg = shift;
  my $recursing = shift;

  if (!$recursing) {
    if (!defined $arg or $arg eq '') {
      # logic not set
      return '';
    }
  }

  if (ref $arg eq 'InterMine::PathQuery::ConstraintSet') {
    my @bits = map {_logic_string($_, 1)} @{$arg->{constraints}};
    my $joined = join ' ' . $arg->{op} . ' ', @bits;
    if ($arg->{op} eq 'and' or not $recursing) {
      return $joined;
    } else {
      return "($joined)";
    }
  } else {
    return $arg->{code};
  }
}

=head2

 Usage   : $path_query->to_xml_string()
 Function: return an XML representation of this path query

=cut
sub to_xml_string
{
  my $self = shift;
  my $name = shift || 'InterMine_Perl_API';
  $self->_is_valid(1);

  my $output = new IO::String();
  my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);

  $writer->startTag('query', 
		    name => $name, 
		    model => $self->{model}->model_name(),
                    view => (join ' ', $self->view()),
                    sortOrder => $self->sort_order(),
                    ($self->{logic}) ? 
		        (constraintLogic => _logic_string($self->{logic}))
                        : '', # prevent an empty tag being set
                    );

  my $current_code = 'A';

  my @constraint_paths = sort keys %{$self->{constraints}};

  for my $path_string (@constraint_paths) {
    
    my $details = $self->{constraints}->{$path_string};
    my $path = new InterMine::Path($self->{model}, $path_string);
    my $type = $path->end_type;
    
# Write the tag
    $writer->startTag(
	'node', 
	 path => $path_string, 
	 type => $type,
    );

    for my $constraint (@$details) {
      my $op = $constraint->{op};
      my $code = $constraint->code();

      if (defined $constraint->{value}) {
        $writer->startTag('constraint', op => $op, value => $constraint->{value},
                          code => $code);
      } else {
        $writer->startTag('constraint', op => $op, code => $code);
      }
      $writer->endTag();
    }
    $writer->endTag();
  }

  $writer->endTag();

  return ${$output->string_ref};
}

sub _is_valid
{
  my $self = shift;
  my $die_on_error = shift;

  if (scalar($self->view()) > 0) {
    return 1;
  } else {
    if ($die_on_error) {
      die "PathQuery is not valid because there no view set\n";
    } else {
      return 0;
    }
  }
}

sub _has_view_path
{
  my $self = shift;
  my $path = shift;

  return grep {$_ eq $path} @{$self->{view}};
}

1;

package InterMine::PathQuery::ConstraintSet;

# dummy package for now, used only so we can bless the result of
# _operator_implementation()

1;

