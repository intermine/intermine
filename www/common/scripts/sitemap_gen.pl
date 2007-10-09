#!/usr/bin/perl

use File::Find;

$dir = $ARGV[0];
$filename="$dir/sitemap.xml";
$url = "http://www.flymine.org";
$urlset = "< urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">";
$head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst)=localtime(time);
$y = $year+1900;
$m = $mon+1;

open(FILE,">$filename") || die("Cannot Open File");
print FILE "$head\n";
print FILE "$urlset\n";
find(\&crawl, $dir);
print FILE "</urlset>";
close(FILE);

 sub crawl {	
   	if ((/\.html$/) || (/\.shtml$/)){
   	
   		$f = $File::Find::name;
   		$f =~ s/$dir//g;

      	print FILE "<url>\n";
       	print FILE "<loc>\n";
        	print FILE "$url$f\n";
	     	print FILE "</loc>\n";
         print FILE "<lastmod>\n";         
        	print FILE "$y-$m-$mday\n";
	     	print FILE "</lastmod>\n"; 	
      	print FILE "</url>\n";
	  }
 }
 
