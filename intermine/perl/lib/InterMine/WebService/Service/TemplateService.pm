

package InterMine::WebService::Service::TemplateService;

=head1 NAME

InterMine::WebService::Service::TemplateService - Service for making InterMine
template queries using the web service

=head1 SYNOPSIS

  my $factory = new InterMine::WebService::ServiceFactory($root, "AppName");
  my $template_service = $factory->get_template_service();

  my $template_service = $InterMine::WebService::TemplateService->new($root, "AppName");
  my ($err, $templates) = $template_service->search_for($keyword);

  my ($err, $templates) = $template_service->list_all();

=head1 AUTHOR

FlyMine C<< <support@flymine.org> >>

=head1 BUGS

Please report any bugs or feature requests to C<support@flymine.org>.

=head1 SUPPORT

You can find documentation for this module with the perldoc command.

    perldoc InterMine::WebService::Service::QueryService

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
use warnings;

use base (qw/InterMine::WebService::Core::Service/);

use IO::String;

use InterMine::WebService::Core::Request;
use InterMine::Template;
use InterMine::WebService::Service::ModelService;

=head2 new

 Usage   : $sevice = InterMine::WebService::Service::TemplateService($service_root,
                                                                  "AppName");
 Function: create a new TemplateService object
 Args    : $service_root - base URL of the web service
           $app_name - application name, tells the server which application uses
                       the service

=cut
sub new
{
  my $class = shift;
  my $service_root = shift;
  my $app_name = shift;

  my $self = $class->SUPER::new($service_root, $app_name);  

  my $ms = InterMine::WebService::Service::ModelService->new($service_root, $app_name);
  $self->{model_service} = $ms;
  bless $self, $class;

  return $self;
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

=head2 get_template

 Usage   : my $template = $service->search_for($name);
 Function: Get the template called $name
 Args    : $name - the exact name of the template you want
 Returns : an InterMine::Template object if successful, or undef
           also returns undef if there are multiple matches 
           try using search_for for multiple identifiers.

=cut

sub get_template {
  my $self      = shift;
  my $name   = shift;
  die "You need a name (try using 'get_templates' if you want them all)\n" 
      unless $name;
  my @templates = $self->get_templates;
  my @templates = grep {$_->get_name eq $name} @templates;
  if (@templates == 1) {
      return shift @templates;
  }
  else { # either no templates or too many (ambiguous)
      return;
  }
}


=head2 

 Usage   : my @all_templates = $service->get_templates;
 Function: get all templates on the server
 Returns : a list of InterMine::Template objects

=cut

sub get_templates {
    my $self = shift;
    unless (exists $self->{'templates'}) { # don't perform multiple fetches
	my $url = $self->get_url().'templates/xml';
	my $request =
	    new InterMine::WebService::Core::Request('GET', $url, 'TEXT');
	
	my $resp = $self->execute_request($request);
	if ($resp->is_error) {
	    die 'Fetching templates failed with message: ', $resp->status_line(), "\n";
	}
	else {
	    my $model = $self->{model_service}->get_model;
	    $self->{'templates'} = $self->_make_templates_from_xml($resp->content, $model);
	}
    }
    return $self->{'templates'};
}

# A private subroutine that processes an xml string containing potentially multiple
# template specifications into an list of InterMine::Template objects

sub _make_templates_from_xml {
    my $self = shift;
    my $xml_string = shift;
    my $model = shift;
    $xml_string =~ s[</?template-queries>][]gs;
    my @templates;
    
    # Cut up the result sting into individual templates
    while ($xml_string =~ m[(<template.*?</template>)(.*)]s) { 
	push @templates, $1;
	$xml_string = $2;	
    }

    return map {InterMine::Template->new(string => $_, model => $model)} @templates;
}


=head2 get_result

 Usage   : my $results = $service->get_result($template, $size);
 Function: get the results of a template
 Args    : $template - an InterMine::Template object
           $size     - the maximum number of results to return
 Returns : HTTP::Response containing the results.

=cut

sub get_result {
    my $self = shift;
    my $template = shift;
    die "get_result needs a valid InterMine::Template\n" 
	unless (ref $template eq 'InterMine::Template');
    my $size = shift || 25; # default is 25
    my $url = $self->get_url().'template/results';
    my $request = 
	InterMine::WebService::Core::Request->new('GET', $url, 'TAB');
    $request->add_parameters(name => $template->get_name);

    # as this is a template, we only need parameters for user editable templates    
    my $i = 1; # these are numbered, starting at 1
    for my $constraint ($template->get_editable_constraints) {
	$request->add_parameters('constraint'.$i => $constraint->get_path);
	$request->add_parameters('op'.$i         => $constraint->op);
	$request->add_parameters('value'.$i      => $constraint->value) 
	    if $constraint->value;
	$request->add_parameters('extraValue'.$i => $constraint->extra_value) 
	    if $constraint->extra_value;
	$i++;
    }

    return $self->execute_request($request);   
}

1;
