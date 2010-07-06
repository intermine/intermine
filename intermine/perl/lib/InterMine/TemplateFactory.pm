package InterMine::TemplateFactory;

=head1 NAME

InterMine::TemplateFactory - Service for making InterMine
template queries using the web service

=head1 SYNOPSIS

  my $factory = InterMine::TemplateFactory->new($xml, $model);

  my @templates = $factory->get_templates;
  my $template  = $factory->get_template($name);
  my @matching_templates = $factory->search_for($keyword);

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::TemplateFactory

You can also look for information at:

=over 4

=item * FlyMine

L<http://www.flymine.org>

=back

=head1 COPYRIGHT & LICENSE

Copyright 2010 FlyMine, all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.

=head1 FUNCTIONS

=cut

use strict;
use warnings;
use Carp;

use InterMine::Template;

sub new {
    my $class = shift;
    my $self  = {};
    if (@_ < 2 || @_ > 3) {
	croak "Bad number of arguments to InterMine::TemplateFactory::new\n";
    }
    
    bless $self, $class;
    
    $self->_construct_tf(@_);

    return $self;
}

sub _construct_tf {
    my ($self, $xml, $model, $invalid) = @_;
    $self->{templates} = _make_templates($xml, $model, $invalid);
    return $self;
}

sub _read_in {
    my $content = shift;
    if ( ($content !~ /\n/) && (-r $content) ) {
	open (my $inputhandle, '<', $content) 
	    or croak "Could not open $content for reading: $!";
	my $xml;
	$xml .= $_ for <$inputhandle>;
	close $inputhandle or croak "Cannot close input file handle! $!";
	return $xml;
    }
    else {
	return $content;
    }
    return;
}


# A private subroutine that processes an xml string containing potentially multiple
# template specifications into an list of InterMine::Template objects


sub _make_templates {
    my ($xml, $model, $invalid) = @_;
    my $xml_validator = qr[(</?template-queries>)];

    my $xml_string = _read_in($xml);
    croak 'Invalid or empty xml' unless ($xml_string && $xml_string =~ /$xml_validator/);

    croak 'Invalid model' unless ($model->isa('InterMine::Model'));

    $xml_string =~ s[</?template-queries>][]gs;
    my @templates;
    
    # Cut up the result sting into individual templates
    while ($xml_string =~ m[(<template.*?</template>)(.*)]s) { 
	push @templates, $1;
	$xml_string = $2;	
    }
    my @returners = map {InterMine::Template->new(
			     string   => $_, 
			     model    => $model,
			     no_validation => $invalid,
			     )} @templates;
    return \@returners;
}

=head2 get_templates()

 Usage   : my @all_templates = $factory->get_templates;
 Function: get all templates on in the factory
 Returns : a list of InterMine::Template objects

=cut


sub get_templates {
    my $self = shift;
    return @{$self->{templates}};
}

=head2 has_templates()

 Usage   : my $bool = $factory->has_templates;
 Function: find out whether there are any templates
 Returns : a truth value

=cut   

sub has_templates {
    my $self = shift;
    return defined $self->{templates};
}


=head2 get_template()

 Usage   : my $template = $service->get_template($name);
 Function: Get the template called $name
 Args    : $name - the exact name of the template you want
 Returns : an InterMine::Template object if successful, or undef
           also returns undef if there are multiple matches 
           try using search_for for multiple identifiers.

=cut

sub get_template {
  my $self   = shift;
  my $name   = shift;
  die "You need a name (try using 'get_templates' if you want them all)\n" 
      unless $name;
  my @templates = $self->get_templates;
  my @wanted = grep {$_->get_name eq $name} @templates;
  if (@wanted == 1) {
      return shift @wanted;
  }
  else { # either no templates or too many (ambiguous)
      return;
  }
}

=head2 search_for

 Usage   : my $templates = $service->search_for($keyword);
 Function: get templates that match search term.
 Args    : $keyword - any term to search by (case insensitive)
 Returns : a list of InterMine::Template objects.

=cut

sub search_for {
  my $self      = shift;
  my $keyword   = shift;
  die "You need a keyword to search for (try using 'get_templates' if you want them all)\n" 
      unless $keyword;
  my @templates = $self->get_templates;
  return grep {$_->get_name =~ /$keyword/i} @templates;
}


1;
