

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

use base qw(InterMine::WebService::Core::Service);

use IO::String;

use InterMine::WebService::Core::Request;
use InterMine::Template;
use InterMine::WebService::Service::ModelService;

my $SERVICE_RELATIVE_URL = 'templates';

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


=head2 get_relative_path

 Usage   : my $rel_path = $service->get_relative_path();
 Function: return the path of this service relative to the base url of the
           webapp

=cut

sub get_relative_path {
    return $SERVICE_RELATIVE_URL;
}


=head2 search_for

 Usage   : my $templates = $service->search_for($keyword);
 Function: get templates that match search term.
 Args    : $keyword - any term to search by, with * as wildcards beginning and end
 Returns : an array of InterMine::Template objects.

=cut
sub search_for
{
  my $self      = shift;
  my $keyword   = shift;
  my @templates = $self->get_templates;
  return grep {$_->get_name =~ /$keyword/i} @templates;
}

sub get_templates {
    my $self = shift;
    
    my $request =
	new InterMine::WebService::Core::Request('GET', $self->get_url().'/xml', 'TEXT');
    
    my $resp = $self->execute_request($request);
    if ($resp->is_error) {
	die 'Fetching templates failed with message: ', $resp->status_line(), "\n";
    }
    else {
	my $model = $self->{model_service}->get_model;
	return $self->_make_templates_from_xml($resp->content, $model);
    }
}

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

1;
