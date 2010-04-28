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
use InterMine::PathQuery;

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
  $self->{template} = $handler->{template};
}

sub get_views {
  my $self = shift;

  my @views = @{$self->{template}{views}};
  
  return @views;
}

sub get_constraints {
    my $self = shift;
    my @constraints = ();
    for my $path (keys %{$self->{template}{paths}}) {
        if (exists $self->{template}{paths}{$path}{constraints}) {
            for my $c (@{$self->{template}{paths}{$path}{constraints}}) {
                push @constraints, $c;
            }
        }
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
    my $desc = $self->{template}{longDescription};
    return $desc;
}

sub get_name {
    my $self = shift;
    my $name = $self->{template}{name};
    return $name;
}

sub get_sort_order {
    my $self = shift;
    my $sort_order = $self->{sort_order};
    return $sort_order;
}

sub get_logic {
    my $self = shift;
    my $logic = $self->{logic};
    return $logic;
}

sub as_path_query {
    my $self = shift;
    my $model = $self->{model};
    my $pq = InterMine::PathQuery->new($model);
    my @views = $self->get_views;
    $pq->add_view(@views);
    $pq->sort_order($self->get_sort_order) 
	if (defined $self->get_sort_order);
    $pq->{logic} = $self->get_logic 
	if (defined $self->get_logic);;
    for my $c ($self->get_constraints) {
	$pq->add_constraint($c);
    }
    return $pq;
}

sub to_xml_string {
    my $self = shift;
    my $pq = $self->as_path_query;
    return $pq->to_xml_string;
}
1;
