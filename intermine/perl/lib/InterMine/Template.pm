package InterMine::Template;

=head1 NAME

InterMine::Template - the representation of an InterMine model

=head1 SYNOPSIS

  use InterMine::Template;

  my $template = new InterMine::Template(file => $template_file,
                                         model => $model);

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

Copyright 2006,2007,2008,2009,2010 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use strict;
use warnings;
use Carp;

use base qw(InterMine::PathQuery);

use XML::Parser::PerlSAX;
use InterMine::PathQuery::Handler;

use IO::String;
use XML::Writer;

=head2 new

 Title   : new
 Usage   : $template = new InterMine::Template(file => $template_file, 
                                              model => $model);
             or
           $template = new InterMine::Template(string => $template_string, 
                                                model => $model);

 Function: return a Template object for the given file/string.
 Args    : file - the InterMine template XML file
             or
           string - the InterMine template XML
           model  - an InterMine::Model

=cut

sub new {
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
	croak "We need a model to build templates with\n";
    }

    $self = _process(%opts);

    bless $self, $class;
    $self->model($opts{model});
    
    $self->_validate unless $opts{no_validation};

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

sub _process {
    my %args = @_;
    my $type = $args{type} || 'template';
    my $handler = InterMine::PathQuery::Handler->new($type);
    my $parser = XML::Parser::PerlSAX->new(Handler => $handler);

    my $source;

    if ($args{string}) {
	$source = {String => $args{string} }; 
    } else {
	$source = {SystemId => $args{file} }; 
    }

    $parser->parse(Source => $source);
    my $q = $handler->{query};

    $q->{source_string} = $args{string} if $args{string}; # TODO: Ugly, ugly, ugly - get rid of
    $q->{type} = $type;

    return $q;
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
    my @ed_constraints = grep {$_->is_editable} $self->get_all_constraints;
    return @ed_constraints;
}

sub get_title {
    my $self = shift;
    my $title = $self->{title};
    return $title;
}


=head2 to_xml_string

 Title   : to_template_xml()
 Usage   : $xml_string = $template->to_template_xml();
 Function: Get an xml representation of the whole template object.
 Args    : no args
 Returns : a string

=cut

sub to_xml_string {
    my $self = shift;

    my $SPACE = ' ';
    my $NEWLINE = "\n";
    my $NOTHING = '',

    # add extra indentation to the query section 
    my @query_lines    = split($NEWLINE, $self->SUPER::to_xml_string);
    my @indented_lines = map {($SPACE x 3) . $_} @query_lines;
    my $query          = join($NEWLINE, $NOTHING, @indented_lines, $NOTHING);

    my $output = new IO::String();
    my $writer = new XML::Writer(UNSAFE => 1, DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);
    
    my %args;
    $args{name}            = $self->get_name();
    $args{'date-created'}  = $self->get_date()        if $self->get_date();
    $args{title}           = $self->get_title()       if $self->get_title;
    $args{longDescription} = $self->get_description() if $self->get_description;
    $args{comment}         = $self->get_comment()     if $self->get_comment;

    $writer->startTag($self->{type}, %args,);
    $writer->raw($query);
    $writer->endTag;
    
    return ${$output->string_ref};
}

sub _validate {
    my $self = shift;
    croak "Invalid template, $!" unless $self->to_xml_string; # This gets all the paths checked   
    croak "Invalid template: no editable constraints" unless $self->get_editable_constraints;
    croak "Invalid template: sort order not in query" unless $self->_validate_sort_order;
}

sub get_comment {
    my $self = shift;
    return $self->{comment};
}

sub get_date {
    my $self = shift;
    return $self->{'date-created'};
}	
1;
