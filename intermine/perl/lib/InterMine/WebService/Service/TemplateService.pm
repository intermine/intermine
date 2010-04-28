

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



sub get_result {
    my $self = shift;
    my $template = shift;
    my $request =
	new InterMine::WebService::Core::Request('GET', $self->get_url().'/results', 'TAB');
    
    $request->add_parameters(name => $template->get_name);

    my $i = 1;
    my @constraints = $template->get_editable_constraints;
    for my $c (@constraints) {
	$request->add_parameters('constraint'.$i => $c->get_path);
	$request->add_parameters('op'.$i         => $c->op);
	$request->add_parameters('value'.$i      => $c->value) if (defined $c->value);
	$request->add_parameters('extraValue'.$i => $c->extra_value) if (defined $c->extra_value);
	$i++;
    }

    $request->add_parameters(size => 10); # change

    my $resp = $self->execute_request($request);
    if ($resp->is_error) {
	die 'Fetching results failed with message: ', $resp->status_line(), "\n";
    }
    else {
	return $resp->content;
    }
}

=head2 get_result_table

 Usage   : my $result_table = $service->get_result_table($query, $start, $max_count);
 Function: return the path of this service relative to the base url of the
           webapp
 Args    : $query - the query as an XML string or as a PathQuery object
           $start - the start row to return, the first row is 1 which is the default
           $max_count - the maximum number of rows to return
 Returns : a table of results as a list of list, eg. (["Gene1", 100], [Gene2, 200])

=cut

sub get_result_table
{
  my $self = shift;
  my $query = shift;
  my $start = shift;
  my $max_count = shift;

  my $response = $self->get_result($query, $start, $max_count);

  if ($response->is_success()) {
    my $io = new IO::String($response->content());

    my @retval = ();

    my $csv = new Text::CSV_XS({sep_char => "\t"});

    while (!$io->eof()) {
      push @retval, $csv->getline ($io);
    }

    return @retval;
  } else {
    die "request failed with error: ", $response->status_line(), "\n",
        $response->content(), "\n";
  }
}

=head2 get_count (NOT IMPLEMENTED)

 Title   : get_count
 Usage   : my $count = $service->get_count($query);
 Function: return the number of result rows for the query
 
 WARNING Support for this feature this feature has been discontinued
 on the server side

=cut
sub get_count
{
# WARNING suport for this feature has been discontinued
  my $self = shift;
  my $query = shift;

  return $self->get_result($query, undef, undef, 1)->content();
}

1;
