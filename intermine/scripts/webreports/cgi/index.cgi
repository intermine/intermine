#!/usr/bin/perl -T

use strict;
use warnings;
use Carp;

use CGI qw(:standard);
my $title = 'InterMine Web-Reports';
print(header,
      start_html( -title => $title,
                  -style =>{src => '/css/style.css'}),
      div({id => 'heading'},
	  div({id => 'banner'}, h1($title)),
	  img({
	       id => 'logo',
	       src => '/chrome/intermine_logo.png',
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
                a({-href => '/webreports/models'},    'InterMine Model Comparison'),
                a({-href => '/webreports/templates'}, 'InterMine Template Collection Comparison'),
                ]
            )
        ),
      ),
	),
      end_html,
     );

exit;
