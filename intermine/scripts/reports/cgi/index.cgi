#!/usr/bin/perl -T

use strict;
use warnings;
use Carp;
use File::Basename;

use CGI qw(:standard);
my $title = 'InterMine Web-Reports';
my $logo_file = '../icons/intermine_logo.png';
my $css_file = '../css/style.css';

print(header,
      start_html( -title => $title,
                  -style => $css_file),
      div({id => 'heading'},
	  div({id => 'banner'}, h1($title)),
	  img({
	       id => 'logo',
	       src => $logo_file,
	       alt => 'Logo',
	       width => 600,
	       height => 75,
	      }),
	  div({class => 'clearall'}, ''),
	 ),
      div({id => 'content'},
        div({id => 'menu'}, 
            ul(li({-type => 'disc'},
                [
#	            a({-href => '/webreports/analytics'}, 'Google Analytics Report on Templates'),
                a({-href => 'models.pl'},    'InterMine Model Comparison'),
                a({-href => 'templates.pl'}, 'InterMine Template Collection Comparison'),
                ]
            )
        ),
      ),
	),
      end_html,
     );

exit;
