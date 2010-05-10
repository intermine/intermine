package InterMine::PathQuery::Constraint;

=head1 NAME

InterMine::PathQuery::Constraint - a constraint on a path in a PathQuery

=head1 SYNOPSIS

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::PathQuery::Constraint

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

# value is 1 for unary operators and 2 for binary operators
my %OPS = ('IS NOT NULL' => 1,
           'IS NULL'     => 1,
           'CONTAINS'    => 2,
           '='           => 2,
           '!='          => 2,
           '<'           => 2,
           '>'           => 2,
           '>='          => 2,
           '<='          => 2,
           'LOOKUP'      => 2,
	   'LIKE'        => 2,
	   'NOT LIKE'    => 2,
	   'IN'          => 2, 
	   'NOT IN'      => 2,
	   # plus five more to come in branch
);

=head2 new

 Usage   : my $con = InterMine::PathQuery::Constraint("= '$department_name'")
 Function: create a new Constraint object
 Args    : $constraint_string - the constraint as text

=cut

sub new
{
  my $class = shift;
  my %args;
  if (@_ == 1) {
      my $constraint_string = shift;
    
      my @bits = $constraint_string =~ 
                m/^(IS\sNOT\sNULL|IS\sNULL|\S+)
                   (?:\s+(.*))?
                 /x;
    
      if (@bits < 1) {
        die "can't parse constraint: $constraint_string\n";
      }
        
      $args{op}    = $bits[0];
      $args{value} = $bits[1];
  }
  else {
      %args = @_;
  }
  if (!exists $OPS{$args{op}}) {
    die qq[unknown operation "$args{op}" in constraint\n];
  }   

  if (defined $args{value}) {
    if ($OPS{$args{op}} == 1) {
      die qq[operator "$args{op}" should not have a value ($args{value})];
    }
    $args{value} =~ s/^'(.*)'$/$1/;
    $args{value} =~ s/^"(.*)"$/$1/;
    
  } else {
    if ($OPS{$args{op}} == 2) {
      die qq[operator "$args{op}" needs a value];
    }
  }

  my $self = {%args};
  
  return bless $self, $class;

}

# Internal method used to set the path. Used by InterMine::PathQuery
sub _set_path {
    my $self = shift;
    my $path = shift;
    die "_set_path needs a path!\n" unless ($path);
    $self->{path} = $path;
    return $self->{path};
}

sub get_path {
    my $self = shift;
    my $path = $self->{path};
    return $path;
}


=head2 op

 Usage   : my $op = $con->op;
           $con-op('LOOKUP');
 Function: Get or set the operation of this constraint (eg. "=", "CONTAINS",
           "IS NULL")
           Clears the current value if you change the operator from 
           binary to unary.

=cut
sub op
{
  my $self = shift;
  my $op = shift;
  if (defined $op) {
      $self->{op} = $op;
      die "Unknown operation: $op" unless $OPS{$op};
      if ($OPS{$op} == 1) {
      $self->{value} = undef;
      }
  }
  return $self->{op};
}

=head2 value

 Usage   : my $val = $con->value;
           $con->value('D. melano*');
 Function: Get or set the value of this constraint if the operator is binary
           Raises an exception (dies) if you set a value to a binary operator.

=cut
sub value
{
  my $self = shift;
  my $value = shift;
  if (defined $value) {
      my $op = $self->{op};
      if ($OPS{$op} == 1) {
      die qq[operator "$op" should not have a value ($value)];
      }
      $self->{value} = $value;
  }  
  return $self->{value};
}

=head2 extra_value

 Usage   : $con->extra_value("D. melanogaster");
           my $extra_value = $con->extra_value;
 Function: Get or set the extra value for a constraint. Returns the new value.

=cut

sub extra_value {
    my $self = shift;
    my $extra_value = shift;
    if (defined $extra_value) {
    $self->{extraValue} = $extra_value;
    }
    return $self->{extraValue};
}

=head2 code

 Usage   : $con->code('A')
              or
           my $code = $con->code();
 Function: get or set the identifier code for this constraint, used to specify the logic
           for a PathQuery

=cut
sub code
{
  my $self = shift;
  my $arg = shift;

  if (defined $arg) {
    $self->{code} = $arg;
  }

  return $self->{code};
}

=head2 is_editable

 Usage   : $con->is_editable;
 Function: Learn whether a constraint can be edited or not (for Templates)
 Returns : True/False values

=cut
sub is_editable {
    my $self = shift;
    return $self->{editable};
}

=head2 as_string

 Usage   : print $con->as_string, "\n";
 Function: Formats the object in a readable string format (similar to the 
           constraint strings that can be used to create new constraints)
 Returns : A string such as 'Homologue.type = "orthologue"'

=cut

sub as_string {
    my $self = shift;
    my $ret = $self->{op};
    $ret    = $self->{path}.' '.$ret if (defined $self->{path});
    $ret   .= ' "'.$self->{value}.'"' if (defined $self->{value});
    $ret   .= ' IN "'.$self->{extraValue}.'"' if (defined $self->{extraValue});
    return $ret;
}
1;
