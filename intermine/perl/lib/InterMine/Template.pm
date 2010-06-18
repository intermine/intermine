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

use IO::String;
use XML::Writer;



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
  my $self = {};

  if (!defined $opts{file} && !defined $opts{string}) {
      die "$class\::new() needs a file or string argument\n";
  }
  elsif (defined $opts{file} && !-f $opts{file}) {
      die "A valid file must be specified: we got $opts{file}\n";
  }
  unless (defined $opts{model}) { 
  # model needed to make the internal PathQuery with
      die "We need a model to build templates with\n";
  }

  bless $self, $class;
 
  $self->{model} = $opts{model};
 
  if (defined $opts{file}) {
    $self->_process($opts{file}, 0);
  } else {
    $self->_process($opts{string}, 1);
    $self->{source_string} = $opts{string};
  }

  return $self;
}

=head2 get_source_string()

 Usage    : my $xmlstring = $template->get_source_string();
 Function : Get the original xml string used to build the template
 Returns  : an string

 This can be useful if changes are made to the values of the template 
 and you wish to revert them, or simply see/store the original version

=cut

sub get_source_string {
    my $self = shift;
    return $self->{source_string};
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

=head2 get_views

 Title   : get_views()
 Usage   : @views = @{ $template->get_views };
 Function: Get a list of the views in the associated PathQuery
 Args    : no args
 Returns : an array of strings 

=cut

sub get_views {
  my $self = shift;
  return $self->{pq}->view;
}


=head2 get_constraints

 Title   : get_constraints()
 Usage   : @constraints = $template->get_constraints;
 Function: get a list of all the constraints in the associated PathQuery.
 Args    : no args
 Returns : an array of InterMine::PathQuery::Constraint objects

 Most of the time the user will be more interested in get_editable_constraints()
 (see below).

=cut

sub get_constraints {
    my $self = shift;
    my @constraints = ();
    my $pq = $self->{pq};
    for my $path (keys %{$pq->{constraints}}) {
	push @constraints, @{$pq->{constraints}{$path}};
    }
    return @constraints;
}


=head2 get_editable_constraints

 Title   : get_editable_constraints()
 Usage   : @editable_constraints = $template->get_editable_constraints;
 Function: get a list of all the constraints in the associated PathQuery 
           which the user can edit.
 Args    : no args
 Returns : an array of InterMine::PathQuery::Constraint objects

=cut

sub get_editable_constraints {
    my $self = shift;
    my @ed_constraints = grep {$_->is_editable} $self->get_constraints;
    return @ed_constraints;
}

=head2 get_description

 Title   : get_description()
 Usage   : $description = $template->get_description();
 Function: Get a human readable informative description of what this
           template does
 Args    : no args
 Returns : a string

=cut

sub get_description {
    my $self = shift;
    my $desc = $self->{pq}->{longDescription};
    return $desc;
}



=head2 get_name

 Title   : get_name()
 Usage   : $name = $template->get_name();
 Function: Get the unique name for the template.
 Args    : no args
 Returns : a string

 This name is the index for the template in the web-application.

=cut

sub get_name {
    my $self = shift;
    my $name = $self->{pq}->{name};
    return $name;
}

sub get_title {
    my $self = shift;
    my $title = $self->{pq}->{title};
    return $title;
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
    my $sort_order = $self->{pq}->{sort_order};
    return $sort_order;
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
    my $logic = $self->{pq}{constraintLogic};
    return $logic;
}


=head2 get_path_query

 Title   : get_path_query()
 Usage   : $path_query = $template->get_path_query();
 Function: Get the underlying PathQuery object
 Args    : no args
 Returns : an InterMine::PathQuery object

=cut


sub as_path_query {
    my $self = shift;
    return $self->{pq};
}


=head2 to_xml_string

 Title   : to_xml_string()
 Usage   : $xml_string = $template->to_xml_string();
 Function: Get an xml representation of the underlying PathQuery object.
 Args    : no args
 Returns : a string

=cut


sub to_xml_string {
    my $self = shift;
    return $self->{pq}->to_xml_string;
}


=head2 to_string

 Title   : to_template_string()
 Usage   : $xml_string = $template->to_template_string();
 Function: Get an xml representation of the whole template object.
 Args    : no args
 Returns : a string

=cut

sub to_string {
    my $self = shift;

    # add extra indentation to the query section 
    my @query_lines = split("\n", $self->to_xml_string);
    for (0 .. @query_lines - 1) {
	$query_lines[$_] = (' ' x 3) . $query_lines[$_];
    }
    my $query  = join("\n",'', @query_lines, '');

    my $output = new IO::String();
    my $writer = new XML::Writer(UNSAFE => 1, DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);
    
    $writer->startTag('template',
		      name            => $self->get_name(),
		      title           => $self->get_title(),
		      longDescription => $self->get_description(),
		      comment         => $self->get_comment(),
	);
    $writer->raw($query);

    $writer->endTag;
    
    return ${$output->string_ref};
}

sub get_comment {
    my $self = shift;
    return $self->{pq}{comment};
}
	
1;
