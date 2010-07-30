#!/usr/bin/perl -T

use strict;
use warnings;
use Carp;

use CGI qw(:standard);

print(header,
      start_html('InterMine Web-Reports'),
      h1('InterMine Web-Reports'),
      p('Available reports:'),
      ul(li({-type => 'disc'},
	    [
#	     a({-href => '/webreports/analytics'}, 'Google Analytics Report on Templates'),
	     a({-href => '/webreports/models'},    'Comparison of Various InterMine models'),
	     a({-href => '/webreports/templates'}, 'Comparison of Various InterMine template collections'),
	    ]
	   )
	),
      end_html,
     );

exit;
