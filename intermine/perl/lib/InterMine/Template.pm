package InterMine::Template;

=head1 NAME

InterMine::Template - the representation of an InterMine model

=head1 SYNOPSIS

  use InterMine::Template;
  use InterMine::ItemFactory;

  my $template_file = 'flymine/dbmodel/build/model/genomic_model.xml';
  my $model = new InterMine::Template(file => $template_file);

  ...

=head1 DESCRIPTION

The class is the Perl representation of an InterMine template.  The
new() method can parse the template file. 

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::Template;

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2006,2007,2008,2009 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use strict;
use warnings;

use XML::Parser::PerlSAX;
use InterMine::Template::Handler;
use InterMine::PathQuery qw(AND OR);

=head2 new

 Title   : new
 Usage   : $template = new InterMine::Template(file => $template_file);
             or
           $template = new InterMine::Template(string => $template_string);
 Function: return a Template object for the given file/string.
 Args    : file - the InterMine template XML file
             or
           string - the InterMine template XML

=cut
sub new
{
  my $class = shift;
  my %opts = @_;
  my $self = {%opts};

  if (!defined $opts{file} && !defined $opts{string}) {
      die "$class\::new() needs a file or string argument\n";
  }
  elsif (defined $opts{file} && !-f $opts{file}) {
      die "A valid file must be specified: we got $opts{file}\n";
  }
  unless (defined $opts{model}) {
      die "We need a model to build templates with\n";
  }

  bless $self, $class;
  
  if (defined $opts{file}) {
    $self->_process($opts{file}, 0);
  } else {
    $self->_process($opts{string}, 1);
  }

  $self->{model} = $opts{model};
  
  return $self;
}


sub _process
{
  my $self = shift;
  my $source_arg = shift;
  my $source_is_string = shift;

  my $handler = InterMine::Template::Handler->new();
  my $parser = XML::Parser::PerlSAX->new(Handler => $handler);

  my $source;

  if ($source_is_string) {
    $source = {String => $source_arg}; 
  } else {
    $source = {SystemId => $source_arg}; 
  }

  $parser->parse(Source => $source);
  my $pq = $handler->{template};
  $pq->{model} = $self->{model};

  bless $pq, 'InterMine::PathQuery';
  $self->{pq} = $pq;

  return;
}

sub _parse_logic {
    # TODO: make this translate human readable logicstrings like "A and B"
    # to args that PQ->logic can understand

# eg: Organism_interologues: (B or G) and (I or F) and J and C and D and E and H and K and L and M and A
    my $self = shift;
    my $logic_string = shift;
    my $triplet = $logic_string;
    my ($left, $op, $right) = $triplet =~ /([A-Z]+)\s(and|or)\s([A-Z]+)/;
    my ($left_constraint) = grep {$_->code eq $left} $self->get_constraints;
    die "$left does not refer to a real constraint" unless (ref $left_constraint);
    my ($right_constraint) = grep {$_->code eq $right} $self->get_constraints;
    die "$right does not refer to a real constraint" unless (ref $right_constraint);
    if ($op eq 'and') {
	return AND ($left_constraint, $right_constraint);
    }
    elsif ($op eq 'or') {
	return OR ($left_constraint, $right_constraint);
    }
    else {
	die qq(Can't understand your logic - "$logic_string": unknown operation);
    }
}

sub get_views {
  my $self = shift;
  return $self->{pq}->view;
}

sub get_constraints {
    my $self = shift;
    my @constraints = ();
    my $pq = $self->{pq};
    for my $path (keys %{$pq->{constraints}}) {
	push @constraints, @{$pq->{constraints}{$path}};
    }
    return @constraints;
}

sub get_editable_constraints {
    my $self = shift;
    my @ed_constraints = grep {$_->is_editable} $self->get_constraints;
    return @ed_constraints;
}

sub get_description {
    my $self = shift;
    my $desc = $self->{pq}->{longDescription};
    return $desc;
}

sub get_name {
    my $self = shift;
    my $name = $self->{pq}->{name};
    return $name;
}

sub get_sort_order {
    my $self = shift;
    my $sort_order = $self->{pq}->{sort_order};
    return $sort_order;
}

sub get_logic {
    my $self = shift;
    my $logic = $self->{pq}{constraintLogic};
    return $logic;
}

sub as_path_query {
    my $self = shift;
    return $self->{pq};
}

sub to_xml_string {
    my $self = shift;
    return $self->{pq}->to_xml_string;
}
1;
