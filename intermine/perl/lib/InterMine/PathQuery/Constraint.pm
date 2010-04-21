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
           # TODO: IN and NOT IN list constraints 
);

=head2 new

 Usage   : my $con = InterMine::PathQuery::Constraint("= '$department_name'")
 Function: create a new Constraint object
 Args    : $constraint_string - the constraint as text

=cut

sub new
{
  my $class = shift;
  my $constraint_string = shift;

  my @bits = $constraint_string =~ 
            m/^(IS NOT NULL|IS NULL|\S+)
               (?:\s+(.*))?
             /x;

  if (@bits < 1) {
    die "can't parse constraint: $constraint_string\n";
  }

  my $op = $bits[0];

  if (!exists $OPS{$op}) {
    die qq[unknown operation "$op" in constraint: $constraint_string\n];
  }

  my $value       = $bits[1];
 
  my $self = {op => $op};

  if (defined $value) {
    if ($OPS{$op} == 1) {
      die qq[operator "$op" should not have a value ($value)];
    }
    $value =~ s/^'(.*)'$/$1/;
    $value =~ s/^"(.*)"$/$1/;
    
    $self->{value} = $value;
  } else {
    if ($OPS{$op} == 2) {
      die qq[operator "$op" needs a value];
    }
  }
  
  return bless $self, $class;

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
	$self->{extra_value} = $extra_value;
    }
    return $self->extra_value;
}

=head2 op

 Usage   : my $op = $con->op;
           $con-op('LOOKUP');
 Function: Get or set the operation of this constraint (eg. "=", "CONTAINS",
           "IS NULL")

=cut
sub op
{
  my $self = shift;
  my $op = shift;
  if (defined $op) {
      $self->{op} = $op;
  }
  return $self->{op};
}

=head2 value

 Usage   : my $val = $con->value();
 Function: return the value of this constraint if the operator is binary

=cut
sub value
{
  my $self = shift;
  return $self->{value};
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
1;
