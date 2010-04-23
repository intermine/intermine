package Template::Handler;

use strict;
use warnings;

sub new {
    my $class = shift;
    my $self = {};
    return bless $self, $class;
}

sub start_element {
    my $self = shift;
    my $args = shift;
    
    my $nameattr = $args->{Attributes}{name};
    if ($args->{Name} eq 'template') {
	$self->{template}{name}  = $nameattr;
	$self->{template}{title} = $args->{Attributes}{title};
	$self->{template}{longDescription} = $args->{Attributes}{longDescription};
	$self->{template}{comment} = $args->{Attributes}{comment};
    }
    else {
	my $template = $self->{template};
	if ($args->{Name} eq 'query') {
	    $template->{model} = $args->{Attributes}{model};
	    my @views = ();
	    if (exists $args->{Attributes}{view}) {
		@views = split(/\s+/, $args->{Attributes}{view});
	    }
	    $template->{views} = \@views;
	}
	elsif ($args->{Name} eq 'node') {
	    $self->{current_node} = $args->{Attributes}{path};
	    print "Current node is ", $self->{current_node}, "\n";
	    $template->{paths}{$args->{Attributes}{path}}{type} = $args->{Attributes}{type};
	}
	elsif ($args->{Name} eq 'constraint') {
	    if ( (exists $args->{Attributes}{editable}) && 
	         ($args->{Attributes}{editable} eq 'true') ) {
		
		my $path = $self->{current_node};
		my $code = $args->{Attributes}{code};
		print "Adding constraint $code to $path\n";
		$template->{paths}{$path}{constraints}{$code}{op} =
		        $args->{Attributes}{op};
		if (exists $args->{Attributes}{value}) {
		    $template->{paths}{$path}{constraints}{$code}{value} = 
			$args->{Attributes}{value};
		}
		if (exists $args->{Attributes}{extraValue}) {
		    $template->{paths}{$path}{constraints}{$code}{extraValue} = 
			$args->{Attributes}{extraValue};
		}
	    }
	}
	elsif (! $args->{Name} eq 'pathDescription') {
	    die "unexpected element: ", $args->{Name}, "\n";
	}
    }
}

sub end_document {
    my $self = shift;
    return($self->{template});
}

1; 

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
use XML::Parser::PerlSAX;

use InterMine::WebService::Core::Request;

my $SERVICE_RELATIVE_URL = "query/results";

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

  bless $self, $class;

  return $self;
}

=head2 get_relative_path

 Usage   : my $rel_path = $service->get_relative_path();
 Function: return the path of this service relative to the base url of the
           webapp

=cut
sub get_relative_path
{
  return "template/results";
}

sub _parse {
    my $input = shift;
    
    my $handler = 
    my parser   = XML::Parser::PerlSAX->new(Handler => $handler);
}

=head2 search_for

 Usage   : my $templates = $service->search_for($keyword);
 Function: get templates that match search term.
 Args    : $keyword - any term to search by, with * as wildcards beginning and end
 Returns : an array of hashes with the template information

=cut
sub search_for
{
  my $self    = shift;
  my $keyword = shift

  my $request =
    new InterMine::WebService::Core::Request('GET', $self->get_url(), 'TEXT');

  $request->add_parameters(keyword => $keyword);

  my $resp = $self->execute_request($request);
  if ($resp->is_error) {
      die 'Fetching templates failed with message: ', $resp->status_line(), "\n";
  }
  else {
      my $handler = new Template::Handler;
      my $parser  = new XML::Parser::PerlSAX(Handler => $handler);
      return $parser->parse($resp->content);
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
