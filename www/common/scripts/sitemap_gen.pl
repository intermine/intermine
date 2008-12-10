#!/usr/bin/perl

use File::Find;

$dir = $ARGV[0];
$filename="$dir/sitemap.xml";

$url = "http://www.flymine.org";
$urlset = "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">";
$head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
$indexfilename = "$dir/sitemap_index.xml";
$indexhead = "<?xml version=\'1.0\' encoding=\'UTF-8\'?>\n<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n xsi:schemaLocation=\"http://www.sitemaps.org/schemas/sitemap/0.9\n http://www.sitemaps.org/schemas/sitemap/0.9/siteindex.xsd\">";

@sitemaps = ("/sitemap.xml", "/query/sitemap.xml");

($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst)=localtime(time);
$y = $year+1900;
$m = sprintf("%02d",$mon+1);	# two digit month
$day = sprintf("%02d",$mday);   # two digit day of month

# write sitemap
open(FILE,">$filename") || die("Cannot Open File");
print FILE "$head\n";
print FILE "$urlset\n";
find(\&crawl, $dir);
print FILE "</urlset>";
close(FILE);

# write sitemap index
open(FILE,">$indexfilename") || die("Cannot Open File");
print FILE "$indexhead\n";
foreach (@sitemaps) {
      	print FILE "<sitemap>\n";
       	print FILE "<loc>";
        print FILE "$url$_";
	print FILE "</loc>\n";
        print FILE "<lastmod>";         
        print FILE "$y-$m-$day";
	print FILE "</lastmod>\n"; 	
      	print FILE "</sitemap>\n";
}
print FILE "</sitemapindex>";
close(FILE);       

 sub crawl {	
    
   	if ((/\.html$/) || (/\.shtml$/)){
   	
   		$f = $File::Find::name;
   		$f =~ s/$dir//g;

      	print FILE "<url>\n";
       	print FILE "<loc>";
       	print FILE "$url$f";
     	print FILE "</loc>\n";
        print FILE "<lastmod>";         
       	print FILE "$y-$m-$day";
     	print FILE "</lastmod>\n"; 	
      	print FILE "</url>\n";
	  }
 }
 
