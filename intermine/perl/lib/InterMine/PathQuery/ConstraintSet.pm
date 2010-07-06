package InterMine::PathQuery::ConstraintSet;

=head1 NAME

InterMine::PathQuery::ConstraintSet - an object representation of a query

=head1 SYNOPSIS

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::PathQuery::ConstraintSet

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

=head2 new

=cut

use strict;
use warnings;
use Carp qw/carp croak confess/;

use base 'InterMine::PathQuery::ConstraintLogic';


sub new {
    my $class = shift; 
    my $logic    = shift;
    my @args     = @_;
    
    unless ($logic) {
	croak "Not enough arguments (min 1)";
    }
    if (@_ > 2) {
	confess "Too many arguments (max 3) - you gave me: ", join(', ', map {'"'.$_.'"'} $logic, @_);
    }

    unless (@_) {
    	if ($logic->isa(__PACKAGE__) ) { # bad logic is caught below
	    return $logic;
	}
    }

    if ($logic !~ /^(and|or)$/) {
	croak "Wrong logical operator - only 'and' and 'or' are allowed, and I got $logic";
    }

    for (@args) {
	_check_logic_arg($_);
    }
    
    my $obj = bless {op => $logic, leftright => [@_]}, $class;    
    return $obj;
}

sub _check_logic_arg {
  my $arg = shift;

  unless ($arg->isa('InterMine::PathQuery::ConstraintLogic') ) {
    my $message = "the argument must be a ConstraintLogic object";
    if (ref $arg) {
      $message .= ", not class: " . ref $arg;
    }
    else {
	$message .= ", but I got $arg";
    }
    croak "$message";
  }
}

sub op {
    return shift->{op};
}

sub leftright {
    my $self = shift;
    return @{$self->{leftright}};
}

sub code {
    return shift->as_string();
}

sub as_string {
    
    my $self = shift;
    my ($left, $right) = map {$_->code} $self->leftright;
    my $string = join ' '.$self->op.' ', $left, $right;
    
   if ($self->op eq 'or') {
	return "($string)";
   }
   else {
    	return $string;
    }
}


1;
